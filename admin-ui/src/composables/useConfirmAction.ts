import { ElMessageBox } from 'element-plus'

export function useConfirmAction() {
  async function confirmAction(message: string, title = '确认操作') {
    try {
      await ElMessageBox.confirm(message, title, {
        type: 'warning',
        confirmButtonText: '继续',
        cancelButtonText: '取消',
      })
      return true
    } catch {
      return false
    }
  }

  return {
    confirmAction,
  }
}
