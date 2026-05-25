package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApexGestordemoApplicationTests {

	@Test
	void applicationClassIsLoadableWithoutDatabase() {
		assertThat(ApexGestordemoApplication.class).isNotNull();
	}

}
