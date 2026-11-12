/** 运行日志接口模块（api-contract §3.12，F-1；FR-C3）。 */
import { request } from './request'
import type { LogEntry, LogQuery, PageResult } from '../types'

/** 业务日志分页查询（类型/模块/成败/时间；本接口 BE 侧不产生日志防递归）。 */
export function listLogs(query: LogQuery): Promise<PageResult<LogEntry>> {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') params.append(k, String(v))
  })
  return request('GET', `/api/v1/logs?${params.toString()}`)
}
