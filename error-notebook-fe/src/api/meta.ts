/** 元数据接口模块（api-contract §3.11，F-1）。 */
import { request } from './request'
import type { EnumsData, KnowledgePointCandidate } from '../types'

/** 枚举字典（yml 配置化；subjectTabs 为分科 Tab，subjects 为录入全集）。 */
export function getEnums(): Promise<EnumsData> {
  return request('GET', '/api/v1/meta/enums')
}

/** 知识点归并候选（BR-03：输入 >=1 字联想，点选复用避免同义分叉）。 */
export function getKnowledgePoints(keyword: string, limit = 10): Promise<KnowledgePointCandidate[]> {
  return request('GET', `/api/v1/meta/knowledge-points?keyword=${encodeURIComponent(keyword)}&limit=${limit}`)
}
