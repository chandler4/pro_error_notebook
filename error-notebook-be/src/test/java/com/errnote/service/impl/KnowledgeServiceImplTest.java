package com.errnote.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.errnote.entity.KnowledgePoint;
import com.errnote.mapper.KnowledgePointMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 知识点字典表驱动单测（TC-C-03 累积 / TC-C-04 候选；SQL 排序语义由 B-11 集成测试兜底）。 */
class KnowledgeServiceImplTest {

  private final KnowledgePointMapper mapper = mock(KnowledgePointMapper.class);
  private final KnowledgeServiceImpl service = new KnowledgeServiceImpl(mapper);

  // TC-C-03 规约3：新名入字典 use_count=1
  @Test
  void newNameIsInsertedWithCountOne() {
    when(mapper.selectOne(any())).thenReturn(null);

    service.accumulate(Collections.singletonList("电压表读数"));

    ArgumentCaptor<KnowledgePoint> captor = ArgumentCaptor.forClass(KnowledgePoint.class);
    verify(mapper).insert(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("电压表读数");
    assertThat(captor.getValue().getUseCount()).isEqualTo(1);
  }

  // TC-C-03 规约3：已存在 use_count+1
  @Test
  void existingNameIncrementsUseCount() {
    KnowledgePoint existing = point("电压表读数", 5);
    when(mapper.selectOne(any())).thenReturn(existing);

    service.accumulate(Collections.singletonList("电压表读数"));

    verify(mapper).updateById(existing);
    assertThat(existing.getUseCount()).isEqualTo(6);
  }

  // TC-C-03 规约3：同一列表同名不重复计（去重后单次累积）
  @Test
  void duplicatedNamesInOneSaveAreCountedOnce() {
    when(mapper.selectOne(any())).thenReturn(point("欧姆定律", 2));

    service.accumulate(Arrays.asList("欧姆定律", "欧姆定律", "欧姆定律"));

    verify(mapper, times(1)).updateById(any(KnowledgePoint.class));
  }

  // 数据生命周期 §5：删除减计、下限 0、不删字典行
  @Test
  void decrementStopsAtZeroAndKeepsRow() {
    KnowledgePoint existing = point("欧姆定律", 1);
    when(mapper.selectOne(any())).thenReturn(existing);

    service.decrement(Arrays.asList("欧姆定律", "欧姆定律"));

    verify(mapper, times(1)).updateById(existing); // 第二次已为 0 不再减
    assertThat(existing.getUseCount()).isEqualTo(0);
    verify(mapper, never()).deleteById(any(java.io.Serializable.class));
  }

  // TC-C-04 规约3（表驱动）：候选映射 + limit 收敛 + 空 keyword 全量
  @Test
  void candidatesMapNamesWithLimitCap() {
    List<KnowledgePoint> records = new ArrayList<>();
    records.add(point("电压表读数", 9));
    records.add(point("串联电路分压规律", 7));
    Page<KnowledgePoint> page = new Page<>(1, 2);
    page.setRecords(records);
    when(mapper.selectPage(any(), any())).thenReturn(page);

    List<String> out = service.candidates("电压", 10);

    assertThat(out).containsExactly("电压表读数", "串联电路分压规律");
  }

  @Test
  void nullAndEmptyInputsAreSafe() {
    service.accumulate(null);
    service.accumulate(Collections.emptyList());
    service.decrement(null);
    verify(mapper, never()).insert(any(KnowledgePoint.class));
    verify(mapper, never()).updateById(any(KnowledgePoint.class));
  }

  private KnowledgePoint point(String name, int useCount) {
    KnowledgePoint kp = new KnowledgePoint();
    kp.setId((long) name.hashCode());
    kp.setName(name);
    kp.setUseCount(useCount);
    return kp;
  }
}
