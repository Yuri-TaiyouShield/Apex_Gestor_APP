package Service;

import DTO.PricingCalculationRequestDTO;
import DTO.PricingCalculationResponseDTO;
import DTO.TenantFeatureContextDTO;
import Model.CommissionPool;
import Model.PricingRule;
import Model.Tenant;
import Repository.CommissionPoolRepository;
import Repository.PricingRuleRepository;
import Repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PricingService {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final String ACTIVE = "ACTIVE";

    private final TenantFeatureService tenantFeatureService;
    private final TenantRepository tenantRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final CommissionPoolRepository commissionPoolRepository;
    private final FinancialAuditTrailService auditTrailService;

    public PricingService(
            TenantFeatureService tenantFeatureService,
            TenantRepository tenantRepository,
            PricingRuleRepository pricingRuleRepository,
            CommissionPoolRepository commissionPoolRepository,
            FinancialAuditTrailService auditTrailService
    ) {
        this.tenantFeatureService = tenantFeatureService;
        this.tenantRepository = tenantRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.commissionPoolRepository = commissionPoolRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PricingCalculationResponseDTO calculate(String requestedTenantCode, PricingCalculationRequestDTO request, String actorLogin) {
        TenantFeatureContextDTO context = tenantFeatureService.resolve(requestedTenantCode);
        String tenantCode = context.tenantCode();
        Tenant tenant = tenantRepository.findByTenantCodeAndStatus(tenantCode, ACTIVE).orElse(null);
        PricingRule rule = pricingRuleRepository
                .findFirstByTenantTenantCodeAndRuleTypeAndStatusOrderByEffectiveFromDesc(tenantCode, "DEFAULT", ACTIVE)
                .orElse(null);

        BigDecimal productCost = positive(request.productCost(), "productCost");
        BigDecimal fixedExpensePercent = firstPresent(request.fixedExpensePercent(), rule == null ? null : rule.getFixedExpensePercent(), BigDecimal.valueOf(8));
        BigDecimal taxPercent = firstPresent(request.taxPercent(), rule == null ? null : rule.getTaxPercent(), BigDecimal.valueOf(6));
        BigDecimal desiredMarginPercent = firstPresent(request.desiredMarginPercent(), rule == null ? null : rule.getDefaultMarginPercent(), BigDecimal.valueOf(20));
        BigDecimal sellerCommissionPercent = firstPresent(request.sellerCommissionPercent(), rule == null ? null : rule.getCommissionPercent(), ZERO);

        boolean commissionRequested = sellerCommissionPercent.compareTo(ZERO) > 0 || request.onlineSale();
        if (commissionRequested) {
            tenantFeatureService.requireFeature(tenantCode, TenantFeatureKey.COMMISSION_OMNICHANNEL);
        } else {
            sellerCommissionPercent = ZERO;
        }

        BigDecimal expenseRate = percent(fixedExpensePercent);
        BigDecimal taxRate = percent(taxPercent);
        BigDecimal marginRate = percent(desiredMarginPercent);
        BigDecimal commissionRate = percent(sellerCommissionPercent);
        BigDecimal totalRate = expenseRate.add(taxRate, MC).add(marginRate, MC).add(commissionRate, MC);
        if (totalRate.compareTo(ONE) >= 0) {
            throw new IllegalArgumentException("A soma de margem, impostos, despesas e comissao deve ser menor que 100%.");
        }

        BigDecimal suggestedPrice = productCost.divide(ONE.subtract(totalRate, MC), MC).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedProfit = suggestedPrice.multiply(marginRate, MC).setScale(2, RoundingMode.HALF_UP);
        CommissionPool pool = commissionRequested && request.onlineSale() && tenant != null
                ? createPool(tenant, request, suggestedPrice, sellerCommissionPercent)
                : null;

        auditTrailService.record(
                "PRECIFICACAO_CALCULADA",
                "pricing_rule",
                rule == null ? null : rule.getIdPricingRule(),
                actor(actorLogin),
                null,
                context.planCode(),
                new PricingCalculationResponseDTO(
                        tenantCode,
                        context.planCode(),
                        productCost,
                        fixedExpensePercent,
                        taxPercent,
                        desiredMarginPercent,
                        sellerCommissionPercent,
                        suggestedPrice,
                        expectedProfit,
                        pool == null ? null : pool.getIdCommissionPool(),
                        "Preco calculado conforme plano e features ativas."
                )
        );

        return new PricingCalculationResponseDTO(
                tenantCode,
                context.planCode(),
                productCost,
                fixedExpensePercent,
                taxPercent,
                desiredMarginPercent,
                sellerCommissionPercent,
                suggestedPrice,
                expectedProfit,
                pool == null ? null : pool.getIdCommissionPool(),
                pool == null ? "Preco calculado sem pool de comissao." : "Preco calculado e pool omnichannel reservado."
        );
    }

    private CommissionPool createPool(Tenant tenant, PricingCalculationRequestDTO request, BigDecimal suggestedPrice, BigDecimal sellerCommissionPercent) {
        BigDecimal gross = request.grossAmount() == null || request.grossAmount().compareTo(ZERO) <= 0 ? suggestedPrice : request.grossAmount();
        BigDecimal poolAmount = gross.multiply(percent(sellerCommissionPercent), MC).setScale(2, RoundingMode.HALF_UP);

        CommissionPool pool = new CommissionPool();
        pool.setTenant(tenant);
        pool.setOrderId(blankToNull(request.orderId()));
        pool.setBranchCode(blankToDefault(request.branchCode(), "matriz"));
        pool.setSourceChannel("ONLINE");
        pool.setGrossAmount(gross);
        pool.setCommissionBase(gross);
        pool.setPoolPercent(sellerCommissionPercent);
        pool.setPoolAmount(poolAmount);
        pool.setStatus("RESERVED");
        pool.setCreatedAt(LocalDateTime.now());
        return commissionPoolRepository.save(pool);
    }

    private BigDecimal firstPresent(BigDecimal value, BigDecimal ruleValue, BigDecimal fallback) {
        if (value != null) {
            return value;
        }
        if (ruleValue != null) {
            return ruleValue;
        }
        return fallback;
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(field + " deve ser maior que zero.");
        }
        return value;
    }

    private BigDecimal percent(BigDecimal value) {
        BigDecimal safe = value == null ? ZERO : value;
        if (safe.abs().compareTo(ONE) > 0) {
            return safe.divide(BigDecimal.valueOf(100), MC);
        }
        return safe;
    }

    private String actor(String actorLogin) {
        return actorLogin == null || actorLogin.isBlank() ? "anonymous" : actorLogin;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
