package Repository;

import Model.CommissionPoolMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionPoolMemberRepository extends JpaRepository<CommissionPoolMember, Long> {

    List<CommissionPoolMember> findByPoolIdCommissionPool(Long poolId);
}
