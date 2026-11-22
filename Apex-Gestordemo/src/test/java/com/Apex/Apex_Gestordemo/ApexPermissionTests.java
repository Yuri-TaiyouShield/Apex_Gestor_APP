package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import com.Apex.Apex_Gestordemo.security.ApexPermission;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApexPermissionTests {

    @Test
    void criticalBusinessPermissionsAreCataloged() {
        Set<String> permissions = Arrays.stream(ApexPermission.values())
                .map(ApexPermission::getCode)
                .collect(Collectors.toSet());

        assertThat(permissions).contains(
                "product.view",
                "product.update",
                "product.price.update",
                "supplier.view",
                "supplier.update",
                "sale.create",
                "sale.cancel",
                "cashier.open",
                "cashier.withdraw",
                "cashier.close",
                "finance.view",
                "settings.global.manage",
                "sale.delete",
                "invoice.cancel",
                "stock.change",
                "finance.change",
                "discount.approve",
                "timeclock.adjust");
    }
}
