package com.pcf.dao;

import com.pcf.model.PcfSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PcfSettingsRepository extends JpaRepository<PcfSettingsEntity, Long> {
}
