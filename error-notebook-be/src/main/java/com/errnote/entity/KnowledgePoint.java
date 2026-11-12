package com.errnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识点字典（knowledge_point 表：唯一名 + use_count，弱关联仅服务归并候选，BR-03）。 */
@Data
@TableName("knowledge_point")
public class KnowledgePoint {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;

  private Integer useCount;

  private LocalDateTime createdAt;
}
