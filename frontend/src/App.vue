<template>
  <div class="dark-theme">
    <div class="bg-animation"></div>
    
    <div class="app-container">
      <!-- 头部 -->
      <div class="header">
        <div class="logo-area">
          <div class="logo-box"><el-icon><Notebook /></el-icon></div>
          <div class="title-group">
            <h1>GALAXY <span class="highlight">CONTACTS</span></h1>
            <span class="subtitle">智能云端通讯录系统</span>
          </div>
        </div>
        <div class="stats-bar">
          <div class="stat-item"><span class="label">成员</span><span class="value">{{ tableData.length }}</span></div>
          <div class="divider"></div>
          <div class="stat-item"><span class="label">星标</span><span class="value active">{{ favCount }}</span></div>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="glass-card toolbar">
        <el-row :gutter="20" align="middle">
          <el-col :span="14" class="left-panel">
            <el-input v-model="searchQuery" placeholder="Type to search..." class="custom-input" :prefix-icon="Search" clearable />
            <div class="filter-tabs">
              <span class="tab-item" :class="{ active: filterType === 'all' }" @click="filterType = 'all'">全部</span>
              <span class="tab-item" :class="{ active: filterType === 'fav' }" @click="filterType = 'fav'">⭐ 仅收藏</span>
            </div>
          </el-col>
          <el-col :span="10" class="right-panel">
            <el-button type="primary" class="neon-btn" @click="openAddDialog" :icon="Plus">新建联系人</el-button>
            <div class="action-group">
              <el-upload class="upload-inline" action="http://localhost:8080/contact/import" :show-file-list="false" :on-success="handleUploadSuccess" :on-error="handleUploadError" accept=".xlsx, .xls">
                <el-button class="ghost-btn" :icon="Upload">导入</el-button>
              </el-upload>
              <el-button class="ghost-btn" @click="handleExport" :icon="Download">导出</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 表格 -->
      <div class="glass-card table-wrapper">
        <el-table :data="filteredTableData" style="width: 100%" v-loading="loading" :default-sort="{ prop: 'createdAt', order: 'descending' }" class="transparent-table">
          <el-table-column label="收藏" width="80" align="center">
            <template #default="scope">
              <div class="fav-btn" :class="{ active: scope.row.isFavorite }" @click.stop="toggleFavorite(scope.row)">
                <el-icon><StarFilled /></el-icon>
              </div>
            </template>
          </el-table-column>
          <el-table-column type="index" label="#" width="60" align="center" />
          <el-table-column prop="name" label="姓名" width="120" sortable>
            <template #default="scope"><span class="name-text">{{ scope.row.name }}</span></template>
          </el-table-column>
          <el-table-column prop="address" label="地址" show-overflow-tooltip />
          <el-table-column label="联系方式" min-width="220">
            <template #default="scope">
              <div v-if="scope.row.details && scope.row.details.length > 0" class="tags-group">
                <span v-for="(item, index) in scope.row.details" :key="index" class="mini-tag" :class="item.type">
                  {{ item.type }}: {{ item.content }}
                </span>
              </div>
              <span v-else class="dim-text">无数据</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" align="center" sortable class-name="time-col" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button type="danger" link class="del-link" @click="handleDelete(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 弹窗：修复了下拉框和按钮白色问题 -->
    <el-dialog v-model="dialogVisible" title="New Member" width="480px" class="cyber-dialog">
      <el-form :model="form" label-width="60px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" class="cyber-input" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" class="cyber-input" />
        </el-form-item>
        
        <div class="detail-box">
          <div class="sub-header">联系方式列表</div>
          <div v-for="(item, index) in form.details" :key="index" class="row-item">
              <!-- 重点修复：cyber-select 样式 -->
              <el-select 
                v-model="item.type" 
                class="cyber-select" 
                style="width: 110px" 
                popper-class="dark-popper"
                :teleported="false"
              >
                  <el-option label="手机" value="手机" />
                  <el-option label="微信" value="微信" />
                  <el-option label="邮箱" value="邮箱" />
              </el-select>
              <el-input v-model="item.content" class="cyber-input flex-1" />
              <el-icon class="remove-icon" @click="removeDetail(index)"><CircleCloseFilled /></el-icon>
          </div>
          <!-- 重点修复：按钮样式 -->
          <div class="add-btn-wrapper" @click="addDetail">
            <el-icon><Plus /></el-icon> 添加一行
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="ghost-btn">取消</el-button>
        <el-button type="primary" @click="submitForm" class="neon-btn">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload, Search, StarFilled, Notebook, CircleCloseFilled } from '@element-plus/icons-vue'

const API_URL = 'http://localhost:8080/contact'
const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const searchQuery = ref('') 
const filterType = ref('all') 
const form = reactive({ name: '', address: '', details: [] })

const filteredTableData = computed(() => {
  let data = tableData.value
  if (filterType.value === 'fav') data = data.filter(item => item.isFavorite)
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    data = data.filter(item => {
      const matchName = item.name?.toLowerCase().includes(query)
      const matchAddress = item.address?.toLowerCase().includes(query)
      const matchDetails = item.details?.some(d => d.content?.toLowerCase().includes(query))
      return matchName || matchAddress || matchDetails
    })
  }
  return data
})
const favCount = computed(() => tableData.value.filter(i => i.isFavorite).length)

// 逻辑不变
const fetchList = async () => { loading.value = true; try { const res = await axios.get(`${API_URL}/list`); tableData.value = res.data } catch (error) { ElMessage.error('服务未连接') } finally { loading.value = false } }
const toggleFavorite = async (row) => { try { await axios.post(`${API_URL}/favorite/${row.id}`); row.isFavorite = !row.isFavorite } catch (error) {} }
const handleExport = () => { window.location.href = `${API_URL}/export` }
const handleUploadSuccess = () => { ElMessage.success('导入成功'); fetchList() }
const handleUploadError = () => { ElMessage.error('格式错误') }
const handleDelete = (id) => { ElMessageBox.confirm('确认删除?', 'Warning', { confirmButtonText: 'Confirm', cancelButtonText: 'Cancel', type: 'warning', customClass: 'cyber-msgbox' }).then(async () => { await axios.delete(`${API_URL}/delete/${id}`); fetchList() }) }
const openAddDialog = () => { form.name = ''; form.address = ''; form.details = [{ type: '手机', content: '' }]; dialogVisible.value = true }
const addDetail = () => { form.details.push({ type: '手机', content: '' }) }
const removeDetail = (index) => { form.details.splice(index, 1) }
const submitForm = async () => { if(!form.name) return ElMessage.warning('请输入姓名'); try { await axios.post(`${API_URL}/save`, { name: form.name, address: form.address, isFavorite: false, details: form.details }); dialogVisible.value = false; fetchList() } catch (error) { ElMessage.error('保存失败') } }
onMounted(() => { fetchList() })
</script>

<style>
/* CSS 全局覆盖 */
:root {
  --el-color-primary: #00d2ff;
  --el-bg-color: #1a1a1a;
  --el-bg-color-overlay: #1e222d;
  --el-text-color-primary: #e0e6ed;
  --el-text-color-regular: #a0a0a0;
  --el-border-color: #333;
  --el-fill-color-blank: #141414; /* 核心：让所有输入框背景变黑 */
  --el-mask-color: rgba(0, 0, 0, 0.8);
}

/* 1. 修复下拉菜单 (Select) 的框体白色问题 */
/* 强制覆盖 Select 内部的 wrapper */
.cyber-select .el-select__wrapper,
.cyber-select .el-input__wrapper {
  background-color: #141414 !important; /* 强制纯黑 */
  box-shadow: 0 0 0 1px #444 inset !important;
}
.cyber-select .el-select__wrapper:hover {
  box-shadow: 0 0 0 1px #00d2ff inset !important;
}

/* 2. 修复下拉菜单弹出的列表 */
.dark-popper.el-popper {
  background: #1e222d !important;
  border: 1px solid #333 !important;
}
.dark-popper .el-select-dropdown__item { color: #ccc; }
.dark-popper .el-select-dropdown__item.hover,
.dark-popper .el-select-dropdown__item:hover { background-color: #2a2a2a; color: #00d2ff; }

/* 3. 修复“添加一行”按钮白色问题 */
/* 我不再使用 el-button，改用 div 模拟按钮，彻底避开 Element 样式干扰 */
.add-btn-wrapper {
  width: 100%;
  background-color: rgba(255, 255, 255, 0.05); /* 透明黑 */
  border: 1px dashed #444;
  color: #888;
  font-size: 13px;
  padding: 8px 0;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  gap: 5px;
}
.add-btn-wrapper:hover {
  background-color: rgba(0, 210, 255, 0.1);
  border-color: #00d2ff;
  color: #00d2ff;
}

/* 其他样式保持 */
.el-input__wrapper { background-color: #141414 !important; box-shadow: 0 0 0 1px #333 inset !important; }
.el-input__inner { color: #fff !important; }
.cyber-dialog { background: #1e222d !important; border: 1px solid #333; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.cyber-dialog .el-dialog__title { color: #fff; }
.cyber-msgbox { background: #1e222d !important; border: 1px solid #333 !important; }
.cyber-msgbox .el-message-box__title, .cyber-msgbox .el-message-box__content { color: #fff !important; }

body { margin: 0; background-color: #0f1115; font-family: 'Inter', sans-serif; color: #e0e6ed; }
.dark-theme { min-height: 100vh; background: radial-gradient(circle at top right, #1a2a3a 0%, #0f1115 60%); padding-bottom: 50px; }
.app-container { max-width: 1100px; margin: 0 auto; padding: 40px 20px; }
.header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 30px; }
.logo-area { display: flex; align-items: center; gap: 16px; }
.logo-box { width: 48px; height: 48px; background: linear-gradient(135deg, #3a7bd5, #00d2ff); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; box-shadow: 0 0 20px rgba(0, 210, 255, 0.3); }
h1 { margin: 0; font-size: 26px; font-weight: 700; }
.highlight { color: #00d2ff; }
.subtitle { font-size: 12px; color: #94a3b8; display: block; margin-top: 4px; }
.stats-bar { display: flex; align-items: center; gap: 20px; background: rgba(0,0,0,0.3); padding: 10px 20px; border-radius: 50px; border: 1px solid #333; }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-item .value { font-size: 18px; font-weight: bold; }
.stat-item .value.active { color: #ffd700; }
.glass-card { background: rgba(30, 34, 45, 0.7); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; box-shadow: 0 8px 32px rgba(0,0,0,0.2); }
.toolbar { padding: 16px; margin-bottom: 24px; }
.left-panel, .right-panel { display: flex; align-items: center; gap: 20px; }
.right-panel { justify-content: flex-end; gap: 15px; }
.action-group { display: flex; gap: 10px; }
.filter-tabs { display: flex; gap: 5px; background: rgba(0,0,0,0.2); padding: 4px; border-radius: 8px; }
.tab-item { padding: 6px 12px; font-size: 13px; color: #888; cursor: pointer; border-radius: 6px; }
.tab-item.active { background: #333; color: #fff; font-weight: 600; }
.neon-btn { background: linear-gradient(90deg, #3a7bd5, #00d2ff) !important; border: none !important; font-weight: 600; color: #fff !important; }
.ghost-btn { background: rgba(255,255,255,0.05) !important; border: 1px solid rgba(255,255,255,0.08) !important; color: #ccc !important; }
.ghost-btn:hover { background: rgba(255,255,255,0.1) !important; color: #fff !important; }
.table-wrapper { padding: 5px; }
.transparent-table { --el-table-bg-color: transparent !important; --el-table-tr-bg-color: transparent !important; --el-table-header-bg-color: rgba(0,0,0,0.2) !important; --el-table-border-color: rgba(255,255,255,0.08) !important; --el-table-text-color: #94a3b8 !important; --el-table-header-text-color: #fff !important; --el-table-row-hover-bg-color: rgba(255,255,255,0.03) !important; background-color: transparent !important; }
.el-table th, .el-table tr, .el-table td { background-color: transparent !important; border-bottom: 1px solid rgba(255,255,255,0.08) !important; }
.name-text { font-weight: 600; color: #fff; }
.mini-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; border: 1px solid rgba(255,255,255,0.1); }
.mini-tag.手机 { color: #4facfe; background: rgba(79, 172, 254, 0.1); }
.mini-tag.微信 { color: #43e97b; background: rgba(67, 233, 123, 0.1); }
.tags-group { display: flex; flex-wrap: wrap; gap: 6px; }
.fav-btn { font-size: 18px; color: #444; cursor: pointer; }
.fav-btn.active { color: #ffd700; filter: drop-shadow(0 0 5px rgba(255, 215, 0, 0.5)); }
.detail-box { background: rgba(0,0,0,0.2); padding: 15px; border-radius: 8px; margin-top: 20px; }
.sub-header { font-size: 12px; color: #666; margin-bottom: 10px; }
.row-item { display: flex; align-items: center; margin-bottom: 8px; gap: 8px; }
.remove-icon { color: #666; cursor: pointer; }
.remove-icon:hover { color: #ff5f5f; }
.upload-inline { display: inline-block; margin-right: 10px; }
</style>