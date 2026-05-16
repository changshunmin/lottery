<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const results = ref([])
const form = ref({ prize: '' })
const claimDialogVisible = ref(false)
const currentResult = ref(null)
const claimForm = ref({ claimerName: '' })

const API_BASE = '/api/lottery'

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

const getPrizeType = (claimed) => {
  return claimed ? 'success' : ''
}

onMounted(() => {
  fetchResults()
})
</script>

<template>
  <div class="lottery-app">
    <el-container>
      <el-header>
        <h1>抽奖管理系统</h1>
      </el-header>
      <el-main>
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
      </el-main>
    </el-container>

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
</style>