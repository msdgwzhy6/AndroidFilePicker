package me.rosuh.filepicker.utils

import android.os.Environment
import me.rosuh.filepicker.bean.FileItemBean
import me.rosuh.filepicker.bean.FileNavBean
import me.rosuh.filepicker.config.FilePickerConfig
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.config.StorageMediaTypeEnum.EXTERNAL_STORAGE
import java.io.File

/**
 *
 * @author rosu
 * @date 2018/11/22
 */
class FileUtils {

    companion object {

        private val pickerConfig by lazy { FilePickerConfig.getInstance(FilePickerManager.instance) }

        /**
         * 根据配置参数获取根目录文件
         * @return File
         */
        fun getRootFile(): File {
            return when (pickerConfig.mediaStorageType) {
                EXTERNAL_STORAGE -> {
                    File(Environment.getExternalStorageDirectory().absoluteFile.toURI())
                }
                else -> {
                    File(Environment.getExternalStorageDirectory().absoluteFile.toURI())
                }
            }
        }

        /**
         * 获取给定文件对象 @param rootFil 下的所有文件，生成列表项对象 @return ArrayList<FileItemBean>
         */
        fun produceListDataSource(rootFile: File): ArrayList<FileItemBean> {
            var listData: ArrayList<FileItemBean>? = ArrayList()

            for (file in rootFile.listFiles()) {
                //以符号 . 开头的视为隐藏文件或隐藏文件夹，后面进行过滤
                val isHiddenFile = file.name.startsWith(".")
                if (file.isDirectory) {
                    listData?.add(FileItemBean(file.name, file.path, false, null, true, isHiddenFile))
                    continue
                }
                val itemBean = FileItemBean(file.name, file.path, false, null, false, isHiddenFile)
                // 如果调用者没有实现文件类型甄别器，则使用默认是的甄别器
                pickerConfig.selfFileType?.fillFileType(itemBean) ?: pickerConfig.defaultFileType.fillFileType(itemBean)
                listData?.add(itemBean)
            }
            // 隐藏文件过滤器
            if (!pickerConfig.isShowHidingFiles) {
                listData = filesHiderFilter(listData!!)
            }

            listData = pickerConfig.selfFilter?.doFilter(listData!!)

            return listData!!
        }

        /**
         * 隐藏文件的过滤器，传入列表的数据集，然后将被视为隐藏文件的条目从中删除
         * @param listData ArrayList<FileItemBean>
         */
        private fun filesHiderFilter(listData: ArrayList<FileItemBean>): ArrayList<FileItemBean> {
            return ArrayList(listData.filter { !it.isHide })
        }

        /**
         * 为导航栏添加数据，也就是每进入一个文件夹，导航栏的列表就添加一个对象
         * 如果是退回到上层文件夹，则删除后续子目录元素
         */
        fun produceNavDataSource(
            currentDataSource: ArrayList<FileNavBean>,
            nextPath: String
        ): ArrayList<FileNavBean> {

            if (currentDataSource.isEmpty()) {
                // 如果为空，为根目录
                currentDataSource.add(
                    FileNavBean(
                        pickerConfig.mediaStorageName,
                        nextPath
                    )
                )
                return currentDataSource
            }

            for (data in currentDataSource) {
                // 如果是回到根目录
                if (nextPath == currentDataSource.first().dirPath) {
                    return ArrayList(currentDataSource.subList(0, 1))
                }
                // 如果是回到当前目录（不包含根目录情况）
                // 直接返回
                val isCurrent = nextPath == currentDataSource[currentDataSource.size - 1].dirPath
                if (isCurrent) {
                    return currentDataSource
                }

                // 如果是回到上层的某一目录(即，当前列表中有该路径)
                // 将列表截取至目标路径元素
                val isBackToAbove = nextPath == data.dirPath
                if (isBackToAbove) {
                    return ArrayList(currentDataSource.subList(0, currentDataSource.indexOf(data) + 1))
                }
            }
            // 循环到此，意味着将是进入子目录
            currentDataSource.add(
                FileNavBean(
                    nextPath.substring(nextPath.lastIndexOf("/") + 1),
                    nextPath
                )
            )
            return currentDataSource
        }
    }
}