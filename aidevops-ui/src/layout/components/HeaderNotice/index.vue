<template>
  <div>
    <el-popover ref="noticePopover" placement="bottom-end" width="320" trigger="manual" :value="noticeVisible" popper-class="notice-popover">
      <div class="notice-header">
        <span class="notice-title">通知公告</span>
        <span class="notice-mark-all" @click="markAllRead">全部已读</span>
      </div>
      <div v-if="noticeLoading" class="notice-loading"><i class="el-icon-loading"></i> 加载中...</div>
      <div v-else-if="noticeList.length === 0" class="notice-empty"><i class="el-icon-inbox"></i><br>暂无公告</div>
      <div v-else>
        <div v-for="item in noticeList" :key="item.noticeId" class="notice-item" :class="{ 'is-read': item.isRead }" @click="previewNotice(item)">
          <el-tag size="mini" :type="item.noticeType === '1' ? 'warning' : 'success'" class="notice-tag">
            {{ item.noticeType === '1' ? '通知' : '公告' }}
          </el-tag>
          <span class="notice-item-title">{{ item.noticeTitle }}</span>
          <span class="notice-item-date">{{ item.createTime }}</span>
        </div>
      </div>
    </el-popover>

    <div v-popover:noticePopover class="right-menu-item hover-effect notice-trigger" @mouseenter="onNoticeEnter" @mouseleave="onNoticeLeave">
      <svg-icon icon-class="bell" />
      <span v-if="unreadCount > 0" class="notice-badge">{{ unreadCount }}</span>
    </div>

    <el-dialog :title="previewTitle" :visible.sync="previewVisible" width="680px" append-to-body custom-class="notice-preview-dialog">
      <div class="notice-preview-meta">
        <el-tag size="small" :type="previewNoticeType === '1' ? 'warning' : 'success'">
          {{ previewNoticeType === '1' ? '通知' : '公告' }}
        </el-tag>
        <span class="notice-preview-info"><i class="el-icon-user"></i> {{ previewCreateBy }}</span>
        <span class="notice-preview-info"><i class="el-icon-time"></i> {{ previewCreateTime }}</span>
      </div>
      <div class="notice-preview-divider"></div>
      <div class="notice-preview-content" v-html="previewContent"></div>
    </el-dialog>
  </div>
</template>

<script>
import { listNoticeTop, markNoticeRead, markNoticeReadAll, getNotice } from '@/api/system/notice'

export default {
  name: 'HeaderNotice',
  data() {
    return {
      noticeList: [],
      unreadCount: 0,
      noticeLoading: false,
      noticeVisible: false,
      noticeLeaveTimer: null,
      previewVisible: false,
      previewTitle: '',
      previewContent: '',
      previewNoticeType: '',
      previewCreateBy: '',
      previewCreateTime: ''
    }
  },
  mounted() {
    this.loadNoticeTop()
  },
  methods: {
    onNoticeEnter() {
      clearTimeout(this.noticeLeaveTimer)
      this.noticeVisible = true
      this.$nextTick(() => {
        const popper = this.$refs.noticePopover.$refs.popper
        if (popper && !popper._noticeBound) {
          popper._noticeBound = true
          popper.addEventListener('mouseenter', () => clearTimeout(this.noticeLeaveTimer))
          popper.addEventListener('mouseleave', () => {
            this.noticeLeaveTimer = setTimeout(() => { this.noticeVisible = false }, 100)
          })
        }
      })
    },
    onNoticeLeave() {
      this.noticeLeaveTimer = setTimeout(() => { this.noticeVisible = false }, 150)
    },
    loadNoticeTop() {
      this.noticeLoading = true
      listNoticeTop().then(res => {
        this.noticeList = res.data || []
        this.unreadCount = res.unreadCount !== undefined ? res.unreadCount : this.noticeList.filter(n => !n.isRead).length
      }).finally(() => {
        this.noticeLoading = false
      })
    },
    previewNotice(item) {
      if (!item.isRead) {
        markNoticeRead(item.noticeId).catch(() => {})
        item.isRead = true
        const idx = this.noticeList.indexOf(item)
        if (idx !== -1) this.$set(this.noticeList, idx, { ...item, isRead: true })
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
      getNotice(item.noticeId).then(res => {
        const notice = res.data
        this.previewTitle = notice.noticeTitle
        this.previewContent = notice.noticeContent
        this.previewNoticeType = notice.noticeType
        this.previewCreateBy = notice.createBy
        this.previewCreateTime = notice.createTime
        this.previewVisible = true
      })
    },
    markAllRead() {
      const ids = this.noticeList.map(n => n.noticeId).join(',')
      if (!ids) return
      markNoticeReadAll(ids).catch(() => {})
      this.noticeList = this.noticeList.map(n => ({ ...n, isRead: true }))
      this.unreadCount = 0
    }
  }
}
</script>

<style lang="scss" scoped>
.notice-trigger {
  position: relative;
  transform: translateX(-6px);
  .svg-icon { width: 1.2em; height: 1.2em; vertical-align: -0.2em; }
  .notice-badge {
    position: absolute;
    top: 7px;
    right: -3px;
    background: #f56c6c;
    color: #fff;
    border-radius: 10px;
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 4px;
    min-width: 16px;
    text-align: center;
    white-space: nowrap;
    pointer-events: none;
  }
}
.notice-popover {
  padding: 0 !important;
  background: linear-gradient(135deg, #0f1728 0%, #18243a 55%, #203456 100%) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.24) !important;
}
.notice-popover .notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(255,255,255,0.04);
  border-bottom: 1px solid rgba(255,255,255,0.08);
  font-size: 13px;
  font-weight: 600;
  color: #eaf2ff;
}
.notice-popover .notice-mark-all {
  font-size: 12px;
  color: #8fd3ff;
  font-weight: normal;
  cursor: pointer;
}
.notice-popover .notice-mark-all:hover { color: #b6e6ff; }
.notice-popover .notice-loading,
.notice-popover .notice-empty {
  padding: 24px;
  text-align: center;
  color: rgba(234, 242, 255, 0.68);
  font-size: 12px;
  line-height: 1.8;
}
.notice-popover .notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  cursor: pointer;
  transition: background 0.15s;
}
.notice-popover .notice-item:last-child { border-bottom: none; }
.notice-popover .notice-item:hover { background: rgba(255,255,255,0.06); }
.notice-popover .notice-item.is-read .notice-tag,
.notice-popover .notice-item.is-read .notice-item-title,
.notice-popover .notice-item.is-read .notice-item-date { opacity: 0.45; filter: grayscale(1); color: #93a6c5; }
.notice-popover .notice-tag { flex-shrink: 0; }
.notice-popover .notice-item-title {
  flex: 1;
  font-size: 12px;
  color: #eaf2ff;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.notice-popover .notice-item-date {
  flex-shrink: 0;
  font-size: 11px;
  color: rgba(234, 242, 255, 0.5);
}
::v-deep .notice-preview-dialog {
  .el-dialog__body { padding: 0 20px 20px; }
  .notice-preview-meta {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 0;
    font-size: 12px;
    color: #a9bddb;
    .notice-preview-info { display: flex; align-items: center; gap: 4px; }
  }
  .notice-preview-divider {
    height: 1px;
    background: linear-gradient(to right, transparent, rgba(255,255,255,0.14), transparent);
    margin-bottom: 16px;
  }
  .notice-preview-content {
    font-size: 14px;
    line-height: 1.85;
    color: #e6efff;
    word-break: break-word;
    img { max-width: 100%; border-radius: 6px; }
    p { margin: 0 0 1em; }
    a { color: #8fd3ff; text-decoration: underline; }
  }
}
</style>
