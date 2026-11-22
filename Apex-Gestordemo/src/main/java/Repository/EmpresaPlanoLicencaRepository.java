package Repository;

import Model.EmpresaPlanoLicenca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaPlanoLicencaRepository extends JpaRepository<EmpresaPlanoLicenca, Long> {

    Optional<EmpresaPlanoLicenca> findByEmpresaId(Long empresaId);

    Optional<EmpresaPlanoLicenca> findByTenantCode(String tenantCode);

    List<EmpresaPlanoLicenca> findByLicenseKeyHash(String licenseKeyHash);
}
