<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const activeTab = ref('results')
const results = ref([])
const form = ref({ prize: '' })
const claimDialogVisible = ref(false)
const currentResult = ref(null)
const claimForm = ref({ claimerName: '' })

const prizes = ref([])
const prizeFormVisible = ref(false)
const editingIndex = ref(-1)
const prizeForm = ref({ name: '', icon: '', probability: null })

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

const submitResult = async () => {
  if (!form.value.prize.trim()) {
    ElMessage.warning('请输入奖品名称')
    return
  }
  try {
    await axios.post(`${API_BASE}/submit`, form.value)
    ElMessage.success('提交成功')
    form.value.prize = ''
    fetchResults()
  } catch (error) {
    ElMessage.error('提交失败')
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

onMounted(() => {
  fetchResults()
  fetchPrizes()
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
            <el-card class="submit-card">
              <template #header>
                <span><el-icon><Plus /></el-icon> 添加抽奖结果</span>
              </template>
              <el-form :model="form" inline>
                <el-form-item label="奖品名称">
                  <el-input v-model="form.prize" placeholder="请输入奖品名称" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitResult">提交</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <el-card class="list-card">
              <template #header>
                <span><el-icon><List /></el-icon> 抽奖结果列表</span>
              </template>
              <el-table :data="results" stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="prize" label="奖品" min-width="200" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.claimedBy ? 'success' : 'warning'">
                      {{ row.claimedBy ? '已领奖' : '未领奖' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="claimedBy" label="领奖人" width="120" />
                <el-table-column prop="createdAt" label="创建时间" width="180">
                  <template #default="{ row }">
                    {{ new Date(row.createdAt).toLocaleString() }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      v-if="!row.claimedBy"
                      type="primary"
                      size="small"
                      @click="openClaimDialog(row)"
                    >
                      领奖
                    </el-button>
                    <span v-else class="claimed-text">已领奖</span>
                  </template>
                </el-table-column>
              </el-table>
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

              <el-table :data="prizes" stripe>
                <el-table-column label="序号" width="60">
                  <template #default="{ $index }">{{ $index + 1 }}</template>
                </el-table-column>
                <el-table-column prop="icon" label="图标" width="60">
                  <template #default="{ row }">
                    <span style="font-size: 24px">{{ row.icon }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="奖品名称" min-width="140" />
                <el-table-column prop="probability" label="中奖概率(%)" width="130">
                  <template #default="{ row }">
                    <span>{{ row.probability }}%</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right">
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

.el-header {
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.el-header h1 {
  margin: 0;
  font-size: 24px;
}

.submit-card {
  margin-bottom: 20px;
}

.claimed-text {
  color: #909399;
  font-size: 14px;
}

.prize-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.prize-header-actions {
  display: flex;
  gap: 8px;
}

.prize-config-card {
  margin-bottom: 20px;
}

.prize-footer {
  margin-top: 20px;
  text-align: center;
}

.prob-hint {
  color: #999;
  font-size: 12px;
  margin-left: 10px;
}
</style>
