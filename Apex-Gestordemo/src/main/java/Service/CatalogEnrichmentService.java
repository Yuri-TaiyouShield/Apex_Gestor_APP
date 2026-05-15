package Service;

import DTO.CatalogEnrichmentResultDTO;
import Model.Produto;
import Repository.ProdutoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class CatalogEnrichmentService {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final ProdutoRepository produtoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${apex.catalog.enrichment.enabled:true}")
    private boolean enabled;

    @Value("${apex.catalog.enrichment.provider-urls:}")
    private String providerUrls;

    @Value("${apex.catalog.enrichment.storage-dir:${user.dir}/storage/catalog}")
    private String storageDir;

    @Value("${apex.catalog.enrichment.public-path:/media/catalog}")
    private String publicPath;

    public CatalogEnrichmentService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Async
    @Transactional
    public CompletableFuture<CatalogEnrichmentResultDTO> enrichAsync(Long produtoId) {
        CatalogEnrichmentResultDTO result = enrich(produtoId);
        return CompletableFuture.completedFuture(result);
    }

    @Transactional
    public CatalogEnrichmentResultDTO enrich(Long produtoId) {
        Optional<Produto> optionalProduto = produtoRepository.findById(produtoId);
        if (optionalProduto.isEmpty()) {
            return new CatalogEnrichmentResultDTO(produtoId, "NOT_FOUND", null, "Produto nao encontrado.");
        }

        Produto produto = optionalProduto.get();
        if (!enabled) {
            produto.setEnriquecimentoCatalogoStatus("DISABLED");
            return new CatalogEnrichmentResultDTO(produtoId, "DISABLED", produto.getImagemUrl(), "Enriquecimento desabilitado.");
        }

        List<String> providers = providerTemplates();
        if (providers.isEmpty()) {
            produto.setEnriquecimentoCatalogoStatus("NO_PROVIDER");
            return new CatalogEnrichmentResultDTO(produtoId, "NO_PROVIDER", produto.getImagemUrl(), "Nenhum provedor autorizado configurado.");
        }

        produto.setEnriquecimentoCatalogoStatus("PENDING");
        String query = catalogQuery(produto);
        for (String provider : providers) {
            Optional<String> imageUrl = searchImageUrl(provider, query);
            if (imageUrl.isEmpty()) {
                continue;
            }
            Optional<String> storedUrl = downloadCompressAndStore(produto, imageUrl.get());
            if (storedUrl.isPresent()) {
                produto.setImagemUrl(storedUrl.get());
                produto.setImagemMimeType("image/jpeg");
                produto.setImagemAtualizadaEm(LocalDateTime.now());
                produto.setEnriquecimentoCatalogoStatus("READY");
                return new CatalogEnrichmentResultDTO(produtoId, "READY", storedUrl.get(), "Imagem oficial vinculada ao catalogo.");
            }
        }

        produto.setEnriquecimentoCatalogoStatus("NOT_FOUND");
        return new CatalogEnrichmentResultDTO(produtoId, "NOT_FOUND", produto.getImagemUrl(), "Nenhuma imagem autorizada encontrada.");
    }

    private List<String> providerTemplates() {
        List<String> providers = new ArrayList<>();
        for (String provider : providerUrls.split(",")) {
            String trimmed = provider.trim();
            if (!trimmed.isBlank()) {
                providers.add(trimmed);
            }
        }
        return providers;
    }

    private String catalogQuery(Produto produto) {
        return String.join(" ", List.of(
                defaultString(produto.getMarca()),
                defaultString(produto.getModelo()),
                defaultString(produto.getDescricao())
        )).trim();
    }

    private Optional<String> searchImageUrl(String template, String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create(template.replace("{query}", encoded));
            if (!isSafeHttpUri(uri)) {
                return Optional.empty();
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .header("User-Agent", "ApexGestorCatalogEnrichment/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            return findImageCandidate(root);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<String> findImageCandidate(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode value = node.get(fieldName);
                if (value != null && value.isTextual() && looksLikeImageField(fieldName, value.asText())) {
                    return Optional.of(value.asText());
                }
                Optional<String> nested = findImageCandidate(value);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                Optional<String> nested = findImageCandidate(item);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }

    private boolean looksLikeImageField(String fieldName, String value) {
        String field = fieldName.toLowerCase(Locale.ROOT);
        String text = value.toLowerCase(Locale.ROOT);
        boolean imageNamedField = field.contains("image") || field.contains("thumbnail") || field.contains("photo") || field.contains("logo");
        boolean imageUrl = text.startsWith("https://") && (text.contains(".jpg") || text.contains(".jpeg") || text.contains(".png") || text.contains(".webp"));
        return imageUrl && (imageNamedField || field.equals("url"));
    }

    private Optional<String> downloadCompressAndStore(Produto produto, String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            if (!isSafeHttpUri(uri)) {
                return Optional.empty();
            }
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "image/*")
                    .header("User-Agent", "ApexGestorCatalogEnrichment/5.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length > MAX_IMAGE_BYTES) {
                return Optional.empty();
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return Optional.empty();
            }
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (original == null) {
                return Optional.empty();
            }

            Path productDir = Path.of(storageDir, "products", String.valueOf(produto.getIdProduto()));
            Files.createDirectories(productDir);
            Path target = productDir.resolve("product-" + produto.getIdProduto() + ".jpg");
            writeJpeg(original, target);
            return Optional.of(publicPath.replaceAll("/$", "") + "/products/" + produto.getIdProduto() + "/" + target.getFileName());
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private void writeJpeg(BufferedImage original, Path target) throws IOException {
        BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(original, 0, 0, null);
        graphics.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(rgb, "jpg", target.toFile());
            return;
        }
        ImageWriter writer = writers.next();
        try (OutputStream outputStream = Files.newOutputStream(target);
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.82f);
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(rgb, null, null), params);
        } finally {
            writer.dispose();
        }
    }

    private boolean isSafeHttpUri(URI uri) {
        if (uri.getScheme() == null || uri.getHost() == null) {
            return false;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(uri.getHost());
            return !address.isAnyLocalAddress()
                    && !address.isLoopbackAddress()
                    && !address.isLinkLocalAddress()
                    && !address.isSiteLocalAddress()
                    && !address.isMulticastAddress();
        } catch (Exception exception) {
            return false;
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
