<template>
  <div>
    <div class="user-info-head" @click="editCropper()">
      <img v-bind:src="options.img" title="点击上传头像" class="img-circle img-lg" />
      <div class="avatar-edit-tip">更换头像</div>
    </div>
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body @opened="modalOpened" @close="closeDialog">
      <el-row class="avatar-editor-row">
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="cropper-pane">
            <vue-cropper
              ref="cropper"
              :img="options.img"
              :info="true"
              :autoCrop="options.autoCrop"
              :autoCropWidth="options.autoCropWidth"
              :autoCropHeight="options.autoCropHeight"
              :fixedBox="options.fixedBox"
              :outputType="options.outputType"
              @realTime="realTime"
              v-if="visible"
            />
          </div>
        </el-col>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <div class="preview-frame">
              <img :src="previews.url" :style="previews.img" />
            </div>
          </div>
        </el-col>
      </el-row>
      <br />
      <el-row class="avatar-toolbar">
        <el-col :lg="3" :sm="4" :xs="5">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              选择
              <i class="el-icon-upload el-icon--right"></i>
            </el-button>
          </el-upload>
        </el-col>
        <el-col :lg="1" :sm="2" :xs="3"><el-button icon="el-icon-plus" size="small" @click="changeScale(1)"></el-button></el-col>
        <el-col :lg="1" :sm="2" :xs="3"><el-button icon="el-icon-minus" size="small" @click="changeScale(-1)"></el-button></el-col>
        <el-col :lg="1" :sm="2" :xs="3"><el-button icon="el-icon-refresh-left" size="small" @click="rotateLeft()"></el-button></el-col>
        <el-col :lg="1" :sm="2" :xs="3"><el-button icon="el-icon-refresh-right" size="small" @click="rotateRight()"></el-button></el-col>
        <el-col :lg="3" :sm="4" :xs="5" :offset="6">
          <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import store from "@/store"
import { VueCropper } from "vue-cropper"
import { uploadAvatar } from "@/api/system/user"
import { debounce } from '@/utils'

export default {
  components: { VueCropper },
  data() {
    return {
      open: false,
      visible: false,
      title: "修改头像",
      options: {
        img: store.getters.avatar,
        autoCrop: true,
        autoCropWidth: 200,
        autoCropHeight: 200,
        fixedBox: true,
        outputType:"png",
        filename: 'avatar'
      },
      previews: {},
      resizeHandler: null
    }
  },
  methods: {
    editCropper() { this.open = true },
    modalOpened() {
      this.visible = true
      if (!this.resizeHandler) {
        this.resizeHandler = debounce(() => { this.refresh() }, 100)
      }
      window.addEventListener("resize", this.resizeHandler)
    },
    refresh() { this.$refs.cropper.refresh() },
    requestUpload() {},
    rotateLeft() { this.$refs.cropper.rotateLeft() },
    rotateRight() { this.$refs.cropper.rotateRight() },
    changeScale(num) {
      num = num || 1
      this.$refs.cropper.changeScale(num)
    },
    beforeUpload(file) {
      if (file.type.indexOf("image/") == -1) {
        this.$modal.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。")
      } else {
        const reader = new FileReader()
        reader.readAsDataURL(file)
        reader.onload = () => {
          this.options.img = reader.result
          this.options.filename = file.name
        }
      }
    },
    uploadImg() {
      this.$refs.cropper.getCropBlob(data => {
        let formData = new FormData()
        formData.append("avatarfile", data, this.options.filename)
        uploadAvatar(formData).then(response => {
          this.open = false
          this.options.img = response.imgUrl
          store.commit('SET_AVATAR', this.options.img)
          this.$modal.msgSuccess("修改成功")
          this.visible = false
        })
      })
    },
    realTime(data) { this.previews = data },
    closeDialog() {
      this.options.img = store.getters.avatar
      this.visible = false
      window.removeEventListener("resize", this.resizeHandler)
    }
  }
}
</script>
<style scoped lang="scss">
.user-info-head {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #101a2d 0%, #1c2a45 100%);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.2);
}

.user-info-head img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-tip {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translateX(-50%);
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.68);
  color: #eaf2ff;
  font-size: 12px;
  opacity: 0;
  transition: opacity .2s ease;
  pointer-events: none;
}

.user-info-head:hover:after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  cursor: pointer;
}
.user-info-head:hover .avatar-edit-tip { opacity: 1; }

.avatar-editor-row { margin-top: 6px; }
.cropper-pane,
.preview-frame {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  overflow: hidden;
}

.avatar-upload-preview {
  width: 100%;
  height: 100%;
}

.preview-frame img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 16px;
}

.avatar-toolbar {
  display: flex;
  align-items: center;
}
</style>
