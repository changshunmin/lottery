<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const activeTab = ref('results')
const results = ref([])
const claimDialogVisible = ref(false)
const currentResult = ref(null)
const claimForm = ref({ claimerName: '' })

const prizes = ref([])
const prizeFormVisible = ref(false)
const editingIndex = ref(-1)
const prizeForm = ref({ name: '', icon: '', probability: null })

// 搜索筛选
const filterPrize = ref('')
const filterUser = ref('')
const filterDateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)

// 表格高度响应式
const tableMaxHeight = ref(600)
const prizeTableMaxHeight = ref(500)
const resultTable = ref(null)

// 工具函数
const parsePrizeName = (prizeStr) => {
  const parts = prizeStr.split(' - ');
  return parts.length >= 2 ? parts.slice(1).join(' - ') : prizeStr;
}
const parseUserName = (prizeStr) => {
  const parts = prizeStr.split(' - ');
  return parts.length >= 2 ? parts[0] : '';
}
const isNonWinning = (name) => name === '谢谢惠顾' || name === '再抽一次';

// 筛选结果
const filteredResults = computed(() => {
  let list = results.value;
  if (filterPrize.value) {
    list = list.filter(r => parsePrizeName(r.prize) === filterPrize.value);
  }
  if (filterUser.value) {
    const q = filterUser.value.toLowerCase();
    list = list.filter(r => parseUserName(r.prize).toLowerCase().includes(q));
  }
  if (filterDateRange.value) {
    const [start, end] = filterDateRange.value;
    const startTime = new Date(start).setHours(0, 0, 0, 0);
    const endTime = new Date(end).setHours(23, 59, 59, 999);
    list = list.filter(r => {
      const t = new Date(r.createdAt).getTime();
      return t >= startTime && t <= endTime;
    });
  }
  return list;
});

// 排序：中奖类别在前，非中奖在后
const sortedResults = computed(() => {
  return [...filteredResults.value].sort((a, b) => {
    const aNonWin = isNonWinning(parsePrizeName(a.prize)) ? 1 : 0;
    const bNonWin = isNonWinning(parsePrizeName(b.prize)) ? 1 : 0;
    if (aNonWin !== bNonWin) return aNonWin - bNonWin;
    return new Date(b.createdAt) - new Date(a.createdAt);
  });
});

// 分页（pageNumber 自动钳制到有效范围，避免筛选后页码越界导致空白页）
const pagedResults = computed(() => {
  const total = sortedResults.value.length;
  const maxPage = Math.max(1, Math.ceil(total / pageSize.value));
  if (currentPage.value > maxPage) currentPage.value = maxPage;
  const start = (currentPage.value - 1) * pageSize.value;
  return sortedResults.value.slice(start, start + pageSize.value);
});

const exportData = () => {
  const data = sortedResults.value
  if (data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const headers = ['奖品', '中奖人', '中奖日期', '领取日期', '领取人']
  const rows = data.map(r => [
    parsePrizeName(r.prize),
    parseUserName(r.prize),
    new Date(r.createdAt).toLocaleString(),
    r.claimedAt ? new Date(r.claimedAt).toLocaleString() : '-',
    r.claimedBy || '-'
  ])
  const csvContent = [headers, ...rows]
    .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const bom = '﻿'
  const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  const dateStr = new Date().toISOString().slice(0, 10)
  link.download = `抽奖结果_${dateStr}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const resetFilters = () => {
  filterPrize.value = ''
  filterUser.value = ''
  filterDateRange.value = null
  currentPage.value = 1
}

// 筛选条件变化时自动回到第一页
watch([filterPrize, filterUser, filterDateRange], () => {
  currentPage.value = 1
})

const API_BASE = '/api/lottery'
const PRIZE_API = '/api/prizes'

const fetchResults = async () => {
  try {
    const response = await axios.get(`${API_BASE}/all`)
    results.value = response.data
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

const openClaimDialog = (result) => {
  currentResult.value = result
  claimForm.value.claimerName = ''
  claimDialogVisible.value = true
}

const claimPrize = async () => {
  if (!claimForm.value.claimerName.trim()) {
    ElMessage.warning('请输入领奖人姓名')
    return
  }
  try {
    await axios.post(`${API_BASE}/${currentResult.value.id}/claim`, claimForm.value)
    ElMessage.success('领奖成功')
    claimDialogVisible.value = false
    fetchResults()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '领奖失败')
  }
}

const fetchPrizes = async () => {
  try {
    const response = await axios.get(PRIZE_API)
    prizes.value = response.data
  } catch (error) {
    ElMessage.error('获取奖品配置失败')
  }
}

const totalProbability = computed(() => {
  return prizes.value.reduce((s, p) => s + (p.probability || 0), 0)
})

const probabilityStatus = computed(() => {
  const diff = Math.abs(totalProbability.value - 100)
  if (diff < 0.01) return { type: 'success', text: `合计 ${totalProbability.value.toFixed(2)}%，完美！` }
  if (diff < 1) return { type: 'warning', text: `合计 ${totalProbability.value.toFixed(2)}%，接近100%` }
  return { type: 'danger', text: `合计 ${totalProbability.value.toFixed(2)}%，请调整至100%` }
})

const openPrizeForm = (index) => {
  if (index >= 0) {
    const p = prizes.value[index]
    prizeForm.value = { name: p.name, icon: p.icon, probability: p.probability }
    editingIndex.value = index
  } else {
    prizeForm.value = { name: '', icon: '🎁', probability: null }
    editingIndex.value = -1
  }
  prizeFormVisible.value = true
}

const savePrizeForm = () => {
  if (!prizeForm.value.name.trim()) {
    ElMessage.warning('请输入奖品名称')
    return
  }
  if (prizeForm.value.probability === null || prizeForm.value.probability < 0) {
    ElMessage.warning('请输入有效的中奖概率')
    return
  }
  const data = {
    name: prizeForm.value.name.trim(),
    icon: prizeForm.value.icon || '🎁',
    probability: Number(prizeForm.value.probability)
  }
  if (editingIndex.value >= 0) {
    prizes.value[editingIndex.value] = { ...prizes.value[editingIndex.value], ...data }
  } else {
    prizes.value.push({ ...data, id: null, sortOrder: prizes.value.length })
  }
  prizeFormVisible.value = false
}

const editPrize = (index) => {
  openPrizeForm(index)
}

const deletePrize = async (index) => {
  try {
    await ElMessageBox.confirm('确定要删除该奖品吗？', '确认删除')
    prizes.value.splice(index, 1)
  } catch {
    // cancelled
  }
}

const saveAllPrizes = async () => {
  if (totalProbability.value <= 0) {
    ElMessage.warning('请至少添加一个奖品')
    return
  }
  try {
    await axios.post(`${PRIZE_API}/batch`, prizes.value.map((p, i) => ({
      name: p.name,
      icon: p.icon,
      probability: p.probability,
      sortOrder: i
    })))
    ElMessage.success('奖品配置已保存')
    fetchPrizes()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const resetPrizes = async () => {
  try {
    await ElMessageBox.confirm('确定要重置为默认奖品配置吗？', '确认重置')
    await axios.post(`${PRIZE_API}/batch`, [
      { name: '奖品随机选', icon: '❓', probability: 0.5, sortOrder: 0 },
      { name: '笔记本电脑', icon: '💻', probability: 1, sortOrder: 1 },
      { name: '谢谢惠顾', icon: '😊', probability: 38.25, sortOrder: 2 },
      { name: '手机', icon: '📱', probability: 2, sortOrder: 3 },
      { name: '充电宝', icon: '🔋', probability: 5, sortOrder: 4 },
      { name: '再抽一次', icon: '🔄', probability: 5, sortOrder: 5 },
      { name: '汇演发票', icon: '🧾', probability: 10, sortOrder: 6 },
      { name: '谢谢惠顾', icon: '😊', probability: 38.25, sortOrder: 7 }
    ])
    ElMessage.success('已重置为默认配置')
    fetchPrizes()
  } catch {
    // cancelled
  }
}

// 响应式表格高度计算
const updateTableHeight = () => {
  const viewportHeight = window.innerHeight
  const headerHeight = 60
  const tabsHeight = 50
  const searchCardHeight = 120
  const paginationHeight = 60
  const padding = 40
  tableMaxHeight.value = Math.max(300, viewportHeight - headerHeight - tabsHeight - searchCardHeight - paginationHeight - padding)
  prizeTableMaxHeight.value = Math.max(250, viewportHeight - headerHeight - tabsHeight - 150 - padding)
}

onMounted(() => {
  fetchResults()
  fetchPrizes()
  updateTableHeight()
  window.addEventListener('resize', updateTableHeight)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateTableHeight)
})
</script>

<template>
  <div class="lottery-app">
    <el-container>
      <el-header>
        <h1>抽奖管理系统</h1>
      </el-header>
      <el-main>
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="抽奖结果" name="results">
            <el-card class="search-card">
              <template #header>
                <span><el-icon><Search /></el-icon> 搜索筛选</span>
              </template>
              <el-form inline>
                <el-form-item label="奖品名称">
                  <el-select v-model="filterPrize" placeholder="全部奖品" clearable style="width:160px">
                    <el-option v-for="p in prizes" :key="p.name" :label="p.name" :value="p.name" />
                  </el-select>
                </el-form-item>
                <el-form-item label="中奖人">
                  <el-input v-model="filterUser" placeholder="输入中奖人" clearable style="width:160px" />
                </el-form-item>
                <el-form-item label="中奖日期">
                  <el-date-picker
                    v-model="filterDateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD"
                    style="width:260px"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="currentPage = 1">搜索</el-button>
                  <el-button @click="resetFilters">重置</el-button>
                  <el-button type="success" @click="exportData">导出</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <el-card class="list-card">
              <template #header>
                <span><el-icon><List /></el-icon> 抽奖结果列表（共 {{ sortedResults.length }} 条）</span>
              </template>
              <el-table :data="pagedResults" stripe ref="resultTable" :max-height="tableMaxHeight">
                <el-table-column label="奖品" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span>{{ parsePrizeName(row.prize) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="中奖人" min-width="80" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span>{{ parseUserName(row.prize) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="中奖日期" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ new Date(row.createdAt).toLocaleString() }}
                  </template>
                </el-table-column>
                <el-table-column label="领取日期" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ row.claimedAt ? new Date(row.claimedAt).toLocaleString() : '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="领取人" min-width="80" show-overflow-tooltip>
                  <template #default="{ row }">
                    <el-tag v-if="row.claimedBy" type="success" size="small">{{ row.claimedBy }}</el-tag>
                    <span v-else class="claimed-text">未领取</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="70" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      v-if="!row.claimedBy && !isNonWinning(parsePrizeName(row.prize))"
                      type="primary"
                      size="small"
                      @click="openClaimDialog(row)"
                    >
                      领奖
                    </el-button>
                    <span v-else-if="row.claimedBy" class="claimed-text">已领奖</span>
                    <span v-else class="nonwin-text">非中奖</span>
                  </template>
                </el-table-column>
              </el-table>
              <div class="pagination-wrapper" v-if="sortedResults.length > pageSize">
                <el-pagination
                  v-model:current-page="currentPage"
                  :page-size="pageSize"
                  :total="sortedResults.length"
                  layout="total, prev, pager, next"
                  background
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="奖品配置" name="prizes">
            <el-card class="prize-config-card">
              <template #header>
                <div class="prize-header">
                  <span><el-icon><Setting /></el-icon> 奖品配置管理</span>
                  <div class="prize-header-actions">
                    <el-button type="default" size="small" @click="resetPrizes">重置默认</el-button>
                    <el-button type="primary" size="small" @click="openPrizeForm(-1)">添加奖品</el-button>
                  </div>
                </div>
              </template>

              <!-- 概率合计提示 -->
              <el-alert
                :type="probabilityStatus.type"
                :title="probabilityStatus.text"
                :closable="false"
                show-icon
                style="margin-bottom: 20px"
              />

              <el-table :data="prizes" stripe :max-height="prizeTableMaxHeight">
                <el-table-column label="序号" width="50">
                  <template #default="{ $index }">{{ $index + 1 }}</template>
                </el-table-column>
                <el-table-column prop="icon" label="图标" width="50">
                  <template #default="{ row }">
                    <span style="font-size: 20px">{{ row.icon }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="奖品名称" min-width="120" show-overflow-tooltip />
                <el-table-column prop="probability" label="中奖概率(%)" width="100">
                  <template #default="{ row }">
                    <span>{{ row.probability }}%</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ $index }">
                    <el-button type="primary" size="small" link @click="editPrize($index)">编辑</el-button>
                    <el-button type="danger" size="small" link @click="deletePrize($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="prize-footer" v-if="prizes.length > 0">
                <el-button type="success" @click="saveAllPrizes" :disabled="totalProbability <= 0">
                  <el-icon><Check /></el-icon> 保存全部配置
                </el-button>
              </div>
              <el-empty v-else description="暂无奖品配置，请添加奖品" />
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>

    <!-- 领奖弹窗 -->
    <el-dialog v-model="claimDialogVisible" title="领奖确认" width="400px">
      <el-form :model="claimForm">
        <el-form-item label="奖品">
          <el-tag>{{ currentResult?.prize }}</el-tag>
        </el-form-item>
        <el-form-item label="领奖人姓名">
          <el-input v-model="claimForm.claimerName" placeholder="请输入姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="claimDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="claimPrize">确认领奖</el-button>
      </template>
    </el-dialog>

    <!-- 奖品编辑弹窗 -->
    <el-dialog v-model="prizeFormVisible" :title="editingIndex >= 0 ? '编辑奖品' : '添加奖品'" width="450px">
      <el-form :model="prizeForm" label-width="100px">
        <el-form-item label="奖品名称">
          <el-input v-model="prizeForm.name" placeholder="请输入奖品名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="prizeForm.icon" placeholder="输入 emoji，如 🎁" maxlength="5" />
        </el-form-item>
        <el-form-item label="中奖概率(%)">
          <el-input-number
            v-model="prizeForm.probability"
            :min="0"
            :max="100"
            :precision="2"
            :step="0.5"
            controls-position="right"
            style="width: 200px"
          />
          <span class="prob-hint">概率总和应接近 100%</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="prizeFormVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrizeForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.lottery-app {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.el-container {
  height: auto;
  min-height: 100vh;
}

.el-header {
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  height: 60px;
}

.el-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.el-main {
  padding: 10px;
}

/* 搜索筛选区域 */
.search-card {
  margin-bottom: 15px;
}

.search-card :deep(.el-card__header) {
  padding: 12px 15px;
  font-size: 15px;
}

.search-card :deep(.el-form) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

/* 列表卡片 */
.list-card {
  margin-bottom: 15px;
}

.list-card :deep(.el-card__header) {
  padding: 12px 15px;
  font-size: 15px;
}

/* 表格响应式优化 */
:deep(.el-table) {
  font-size: 13px;
}

:deep(.el-table th) {
  padding: 10px 5px;
  font-size: 13px;
}

:deep(.el-table td) {
  padding: 8px 5px;
}

:deep(.el-table .cell) {
  padding: 0 5px;
  word-break: break-word;
}

.claimed-text {
  color: #909399;
  font-size: 12px;
}

.nonwin-text {
  color: #c0c4cc;
  font-size: 12px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

:deep(.el-pagination) {
  padding: 10px 0;
}

/* 奖品配置区域 */
.prize-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.prize-header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.prize-config-card {
  margin-bottom: 15px;
}

.prize-config-card :deep(.el-card__header) {
  padding: 12px 15px;
}

.prize-footer {
  margin-top: 15px;
  text-align: center;
}

.prob-hint {
  color: #999;
  font-size: 11px;
  margin-left: 8px;
  display: block;
  margin-top: 5px;
}

/* 弹窗响应式 */
:deep(.el-dialog) {
  max-width: 90vw;
  margin-top: 5vh !important;
}

:deep(.el-dialog__header) {
  padding: 15px;
}

:deep(.el-dialog__body) {
  padding: 15px;
}

:deep(.el-dialog__footer) {
  padding: 10px 15px;
}

/* Tab 响应式 */
:deep(.el-tabs__header) {
  margin-bottom: 10px;
}

:deep(.el-tabs__item) {
  padding: 0 15px;
  font-size: 14px;
}

/* ===== 移动端适配 - 小屏幕 (≤768px) ===== */
@media screen and (max-width: 768px) {
  .el-header {
    height: 50px;
  }
  
  .el-header h1 {
    font-size: 16px;
  }
  
  .el-main {
    padding: 8px;
  }
  
  /* 搜索表单堆叠布局 */
  .search-card :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }
  
  .search-card :deep(.el-select),
  .search-card :deep(.el-input),
  .search-card :deep(.el-date-editor) {
    width: 100% !important;
  }
  
  .search-card :deep(.el-form-item:last-child) {
    width: 100%;
    display: flex;
    gap: 8px;
  }
  
  .search-card :deep(.el-form-item:last-child .el-button) {
    flex: 1;
  }
  
  /* 表格横向滚动 */
  :deep(.el-table) {
    font-size: 12px;
  }
  
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 6px 3px;
  }
  
  /* 操作按钮优化 */
  :deep(.el-button--small) {
    padding: 5px 8px;
    font-size: 11px;
  }
  
  /* 标签优化 */
  :deep(.el-tag) {
    font-size: 11px;
    padding: 2px 6px;
  }
  
  /* 分页紧凑 */
  :deep(.el-pagination) {
    --el-pagination-font-size: 12px;
    --el-pagination-button-size: 28px;
  }
  
  /* 奖品配置表格优化 */
  .prize-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .prize-header-actions {
    width: 100%;
    justify-content: flex-start;
  }
  
  .prize-header-actions .el-button {
    flex: 1;
  }
  
  /* 弹窗全屏 */
  :deep(.el-dialog) {
    width: 95vw !important;
    max-width: none !important;
    margin-top: 2vh !important;
  }
  
  :deep(.el-dialog__title) {
    font-size: 16px;
  }
  
  /* 表单项堆叠 */
  :deep(.el-form-item__label) {
    font-size: 13px;
  }
  
  :deep(.el-input-number) {
    width: 100% !important;
  }
  
  .prob-hint {
    margin-left: 0;
  }
}

/* ===== 超小屏幕 (≤480px) ===== */
@media screen and (max-width: 480px) {
  .el-header h1 {
    font-size: 14px;
  }
  
  :deep(.el-tabs__item) {
    padding: 0 10px;
    font-size: 13px;
  }
  
  /* 表格进一步优化 */
  :deep(.el-table) {
    font-size: 11px;
  }
  
  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 5px 2px;
  }
  
  /* 简化表格列显示 - 隐藏次要列 */
  :deep(.el-table .el-table__row) {
    /* 可通过添加 class 控制特定列的显示/隐藏 */
  }
  
  /* 按钮文字优化 */
  :deep(.el-button span) {
    font-size: 12px;
  }
}

/* ===== 中等屏幕 (769px - 1024px) ===== */
@media screen and (min-width: 769px) and (max-width: 1024px) {
  .el-header h1 {
    font-size: 18px;
  }
  
  .search-card :deep(.el-select),
  .search-card :deep(.el-input) {
    width: 140px !important;
  }
  
  .search-card :deep(.el-date-editor) {
    width: 200px !important;
  }
}
</style>
