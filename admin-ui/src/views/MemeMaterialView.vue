<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createMemeMaterial,
  listMemeMaterials,
  listScenes,
  preheatMemeCache,
  saveScene,
  updateMemeMaterial,
  type MemeMaterial,
  type MemeMaterialRequest,
  type SceneDict,
} from '@/api/memes'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const saving = ref(false)
const scenes = ref<SceneDict[]>([])
const materials = ref<MemeMaterial[]>([])
const selected = ref<MemeMaterial | null>(null)
const filter = reactive({
  sceneCode: '',
  enabled: '',
  keyword: '',
})
const sceneForm = reactive({
  sceneCode: '',
  sceneDesc: '',
  confidenceThreshold: 0.75,
})
const materialForm = reactive<MemeMaterialRequest>({
  keywords: '',
  sceneCode: '',
  sceneDesc: '',
  weight: 1,
  enabled: true,
  filePath: '',
})

const enabledFilter = computed(() => {
  if (filter.enabled === 'true') return true
  if (filter.enabled === 'false') return false
  return null
})

async function loadAll() {
  loading.value = true
  try {
    const [sceneList, materialList] = await Promise.all([
      listScenes(),
      listMemeMaterials({
        sceneCode: filter.sceneCode || null,
        enabled: enabledFilter.value,
        keyword: filter.keyword || null,
      }),
    ])
    scenes.value = sceneList
    materials.value = materialList
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '表情包素材加载失败')
  } finally {
    loading.value = false
  }
}

async function saveSceneForm() {
  const sceneCode = sceneForm.sceneCode.trim()
  if (!sceneCode || !sceneForm.sceneDesc.trim()) {
    ElMessage.warning('请填写 sceneCode 和场景描述')
    return
  }
  saving.value = true
  try {
    await saveScene(sceneCode, {
      sceneDesc: sceneForm.sceneDesc.trim(),
      confidenceThreshold: Number(sceneForm.confidenceThreshold),
    })
    ElMessage.success('场景已保存，缓存已尝试刷新')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '场景保存失败')
  } finally {
    saving.value = false
  }
}

async function saveMaterialForm() {
  if (!materialForm.filePath?.trim()) {
    ElMessage.warning('请填写素材相对路径')
    return
  }
  saving.value = true
  try {
    const request = normalizeMaterial()
    const saved = selected.value
      ? await updateMemeMaterial(selected.value.memeId, request)
      : await createMemeMaterial(request)
    selected.value = saved
    applyMaterial(saved)
    ElMessage.success('素材已保存，缓存已尝试刷新')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '素材保存失败')
  } finally {
    saving.value = false
  }
}

async function refreshCache() {
  saving.value = true
  try {
    await preheatMemeCache()
    ElMessage.success('表情包缓存已尝试刷新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '缓存刷新失败')
  } finally {
    saving.value = false
  }
}

function selectMaterial(item: MemeMaterial) {
  selected.value = item
  applyMaterial(item)
}

function newMaterial() {
  selected.value = null
  Object.assign(materialForm, {
    keywords: '',
    sceneCode: filter.sceneCode || '',
    sceneDesc: sceneDescOf(filter.sceneCode),
    weight: 1,
    enabled: true,
    filePath: filter.sceneCode ? `${filter.sceneCode}/${filter.sceneCode}_001.png` : '',
  })
}

function editScene(item: SceneDict) {
  sceneForm.sceneCode = item.sceneCode
  sceneForm.sceneDesc = item.sceneDesc
  sceneForm.confidenceThreshold = Number(item.confidenceThreshold)
  filter.sceneCode = item.sceneCode
  materialForm.sceneCode = item.sceneCode
  materialForm.sceneDesc = item.sceneDesc
  loadAll()
}

function applyMaterial(item: MemeMaterial) {
  Object.assign(materialForm, {
    keywords: item.keywords ?? '',
    sceneCode: item.sceneCode ?? '',
    sceneDesc: item.sceneDesc ?? '',
    weight: item.weight ?? 1,
    enabled: Boolean(item.enabled),
    filePath: item.filePath ?? '',
  })
}

function normalizeMaterial(): MemeMaterialRequest {
  const sceneCode = materialForm.sceneCode?.trim() || null
  return {
    keywords: materialForm.keywords?.trim() || '',
    sceneCode,
    sceneDesc: materialForm.sceneDesc?.trim() || sceneDescOf(sceneCode),
    weight: Math.max(0, Number(materialForm.weight || 0)),
    enabled: Boolean(materialForm.enabled),
    filePath: materialForm.filePath.trim(),
  }
}

function sceneDescOf(sceneCode?: string | null) {
  if (!sceneCode) return null
  return scenes.value.find((item) => item.sceneCode === sceneCode)?.sceneDesc ?? null
}

function copySceneToMaterial() {
  materialForm.sceneDesc = sceneDescOf(materialForm.sceneCode) ?? ''
}

onMounted(loadAll)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="表情包素材"
      description="维护 scene_dict 和 meme_material：关键词、场景、权重、启用状态和图片相对路径。"
    >
      <template #eyebrow>Meme Assets</template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button :loading="saving" @click="refreshCache">刷新缓存</el-button>
      </template>
    </PageHeader>

    <section class="meme-layout">
      <aside class="panel meme-side">
        <div class="panel-title-row">
          <h3>场景字典</h3>
          <span class="panel-subtitle">scene_dict</span>
        </div>
        <div class="scene-list">
          <button
            v-for="item in scenes"
            :key="item.sceneCode"
            class="scene-item"
            type="button"
            :class="{ active: filter.sceneCode === item.sceneCode }"
            @click="editScene(item)"
          >
            <span>{{ item.sceneCode }}</span>
            <small>{{ item.sceneDesc }}</small>
          </button>
        </div>
        <el-form class="scene-form" label-position="top">
          <el-form-item label="sceneCode">
            <el-input v-model="sceneForm.sceneCode" placeholder="laugh" clearable />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="sceneForm.sceneDesc" placeholder="好笑、调侃场景" clearable />
          </el-form-item>
          <el-form-item label="置信度阈值">
            <el-input-number v-model="sceneForm.confidenceThreshold" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="saveSceneForm">保存场景</el-button>
        </el-form>
      </aside>

      <section class="panel">
        <div class="panel-title-row">
          <h3>素材列表</h3>
          <span class="panel-subtitle">meme_material</span>
        </div>
        <el-form class="meme-filter" label-position="top">
          <el-form-item label="场景">
            <el-select v-model="filter.sceneCode" clearable @change="loadAll">
              <el-option
                v-for="item in scenes"
                :key="item.sceneCode"
                :label="item.sceneCode"
                :value="item.sceneCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filter.enabled" clearable @change="loadAll">
              <el-option label="启用" value="true" />
              <el-option label="停用" value="false" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filter.keyword" placeholder="哈哈" clearable @keyup.enter="loadAll" />
          </el-form-item>
          <el-form-item class="rank-submit">
            <div class="table-actions">
              <el-button :icon="Search" type="primary" :loading="loading" @click="loadAll">查询</el-button>
              <el-button @click="newMaterial">新增素材</el-button>
            </div>
          </el-form-item>
        </el-form>

        <el-table v-if="materials.length" :data="materials" class="rank-table" @row-click="selectMaterial">
          <el-table-column prop="memeId" label="ID" width="76" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sceneCode" label="场景" width="110" />
          <el-table-column label="关键词" min-width="180">
            <template #default="{ row }">
              <div class="table-snippet">{{ row.keywords || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重" width="80" />
          <el-table-column label="路径" min-width="260">
            <template #default="{ row }">
              <div class="table-title">{{ row.filePath }}</div>
              <div class="table-snippet">{{ row.sceneDesc || '-' }}</div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-block compact">暂无素材。可以先新增一条相对路径素材。</div>
      </section>

      <section class="panel meme-editor">
        <div class="panel-title-row">
          <h3>{{ selected ? `编辑素材 #${selected.memeId}` : '新增素材' }}</h3>
          <span class="panel-subtitle">保存后会尝试刷新 Redis 缓存</span>
        </div>
        <el-form class="config-form" label-position="top">
          <el-form-item label="图片相对路径">
            <el-input v-model="materialForm.filePath" placeholder="laugh/laugh_001.png" clearable />
          </el-form-item>
          <div class="number-form-grid">
            <el-form-item label="场景">
              <el-select v-model="materialForm.sceneCode" clearable @change="copySceneToMaterial">
                <el-option
                  v-for="item in scenes"
                  :key="item.sceneCode"
                  :label="item.sceneCode"
                  :value="item.sceneCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="权重">
              <el-input-number v-model="materialForm.weight" :min="0" />
            </el-form-item>
            <el-form-item label="启用">
              <el-switch v-model="materialForm.enabled" />
            </el-form-item>
          </div>
          <el-form-item label="关键词">
            <el-input
              v-model="materialForm.keywords"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 5 }"
              placeholder="哈哈,笑死,乐"
            />
          </el-form-item>
          <el-form-item label="场景描述">
            <el-input v-model="materialForm.sceneDesc" placeholder="可从 scene_dict 自动带出" clearable />
          </el-form-item>
          <div class="table-actions">
            <el-button type="primary" :loading="saving" @click="saveMaterialForm">保存素材</el-button>
            <el-button @click="newMaterial">清空为新增</el-button>
          </div>
        </el-form>
      </section>
    </section>
  </div>
</template>
