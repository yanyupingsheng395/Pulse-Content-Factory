package com.pcf.dao;

import com.pcf.model.ContentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentTaskRepository extends JpaRepository<ContentTask, Long> {

    Optional<ContentTask> findByShareUrl(String shareUrl);
}
