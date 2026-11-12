package com.errnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.errnote.entity.KnowledgePoint;
import com.errnote.mapper.KnowledgePointMapper;
import com.errnote.service.KnowledgeService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 知识点字典实现：单用户场景无并发争用；字典永久累积不删行（BR-03/数据生命周期）。 */
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

  private final KnowledgePointMapper mapper;

  @Override
  @Transactional
  public void accumulate(List<String> knowledgePoints) {
    if (knowledgePoints == null || knowledgePoints.isEmpty()) {
      return;
    }
    // TC-C-03：同名不重复（同一列表去重后逐名累积）
    for (String name : new LinkedHashSet<>(knowledgePoints)) {
      if (name == null || name.trim().isEmpty()) {
        continue;
      }
      KnowledgePoint existing = mapper.selectOne(nameEquals(name));
      if (existing == null) {
        KnowledgePoint created = new KnowledgePoint();
        created.setName(name);
        created.setUseCount(1);
        created.setCreatedAt(LocalDateTime.now());
        mapper.insert(created);
      } else {
        existing.setUseCount(existing.getUseCount() + 1);
        mapper.updateById(existing);
      }
    }
  }

  @Override
  @Transactional
  public void decrement(List<String> knowledgePoints) {
    if (knowledgePoints == null || knowledgePoints.isEmpty()) {
      return;
    }
    for (String name : new LinkedHashSet<>(knowledgePoints)) {
      if (name == null || name.trim().isEmpty()) {
        continue;
      }
      KnowledgePoint existing = mapper.selectOne(nameEquals(name));
      if (existing != null && existing.getUseCount() > 0) {
        existing.setUseCount(existing.getUseCount() - 1);
        mapper.updateById(existing);
      }
    }
  }

  @Override
  public List<String> candidates(String keyword, int limit) {
    LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<>();
    if (keyword != null && !keyword.trim().isEmpty()) {
      wrapper.like(KnowledgePoint::getName, keyword.trim()); // 包含匹配
    }
    wrapper.orderByDesc(KnowledgePoint::getUseCount); // 热度降序（TC-C-04）
    int safeLimit = Math.max(1, Math.min(limit, 50));
    Page<KnowledgePoint> page = mapper.selectPage(new Page<>(1, safeLimit), wrapper);
    return page.getRecords().stream().map(KnowledgePoint::getName).collect(Collectors.toList());
  }

  private LambdaQueryWrapper<KnowledgePoint> nameEquals(String name) {
    return new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getName, name);
  }
}
