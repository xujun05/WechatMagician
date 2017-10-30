@file:Suppress("MemberVisibilityCanPrivate")

package com.gh0u1l5.wechatmagician.backend

import android.content.Context
import com.gh0u1l5.wechatmagician.C
import com.gh0u1l5.wechatmagician.Global.WECHAT_PACKAGE_NAME
import com.gh0u1l5.wechatmagician.Version
import com.gh0u1l5.wechatmagician.util.PackageUtil.findClassIfExists
import com.gh0u1l5.wechatmagician.util.PackageUtil.findClassesFromPackage
import com.gh0u1l5.wechatmagician.util.PackageUtil.findFieldsWithType
import com.gh0u1l5.wechatmagician.util.PackageUtil.findMethodsByExactParameters
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.bean.DexClass

// WechatPackage analyzes and stores critical classes and objects in Wechat application.
// These classes and objects will be used for hooking and tampering with runtime data.
object WechatPackage {

    var XLogSetup: Class<*>? = null
    var WXCustomSchemeEntry: Class<*>? = null
    var WXCustomSchemeEntryStart = ""
    var EncEngine: Class<*>? = null
    var EncEngineEDMethod = ""

    var SQLiteDatabasePkg = ""
    var SQLiteDatabaseClass: Class<*>? = null
    var SQLiteCursorFactory: Class<*>? = null
    var SQLiteErrorHandler: Class<*>? = null
    var SQLiteCancellationSignal: Class<*>? = null

    var MMActivity: Class<*>? = null
    var MMFragmentActivity: Class<*>? = null
    var MMListPopupWindow: Class<*>? = null

    var PLTextView: Class<*>? = null
    var RemittanceAdapter: Class<*>? = null

    var SnsUploadUI: Class<*>? = null
    var SnsUploadUIEditTextField = ""
    var AdFrameLayout: Class<*>? = null
    var SnsPostTextView: Class<*>? = null
    var SnsPhotosContent: Class<*>? = null

    var AlbumPreviewUI: Class<*>? = null
    var SelectContactUI: Class<*>? = null
    var SelectConversationUI: Class<*>? = null
    var SelectConversationUIMaxLimitMethod = ""

    var MsgInfoClass: Class<*>? = null
    var ContactInfoClass: Class<*>? = null
    var MsgStorageClass: Class<*>? = null
    var MsgStorageInsertMethod = ""
    @Volatile var MsgStorageObject: Any? = null

    var XMLParserClass: Class<*>? = null
    var XMLParseMethod = ""

    val CacheMapClass = "$WECHAT_PACKAGE_NAME.a.f"
    val CacheMapPutMethod = "k"

    var ImgStorageClass: Class<*>? = null
    var ImgStorageCacheField = ""
    val ImgStorageLoadMethod = "a"
    @Volatile var ImgStorageObject: Any? = null

    // Analyzes Wechat package statically for the name of classes.
    // WechatHook will do the runtime analysis and set the objects.
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        val loader = lpparam.classLoader
        val version = getVersion(lpparam)

        var apkFile: ApkFile? = null
        val classes: Array<DexClass>
        try {
            apkFile = ApkFile(lpparam.appInfo.sourceDir)
            classes = apkFile.dexClasses
        } finally {
            apkFile?.close()
        }

        XLogSetup = findClassIfExists(
                "$WECHAT_PACKAGE_NAME.xlog.app.XLogSetup", loader)
        WXCustomSchemeEntry = findClassIfExists(
                "$WECHAT_PACKAGE_NAME.plugin.base.stub.WXCustomSchemeEntryActivity", loader)
        WXCustomSchemeEntryStart = findMethodsByExactParameters(
                WXCustomSchemeEntry, C.Boolean, C.Intent
        ).firstOrNull()?.name ?: ""

        EncEngine = findClassesFromPackage(loader, classes, "$WECHAT_PACKAGE_NAME.modelsfs")
                .filterByMethod(null, "seek", C.Long)
                .filterByMethod(null, "free")
                .firstOrNull("EncEngine")
        EncEngineEDMethod = findMethodsByExactParameters(
                EncEngine, C.Int, C.ByteArray, C.Int
        ).firstOrNull()?.name ?: ""

        SQLiteDatabasePkg = when {
            version >= Version("6.5.8") ->"com.tencent.wcdb"
            else ->"com.tencent.mmdb"
        }
        SQLiteDatabaseClass = findClassIfExists(
                "$SQLiteDatabasePkg.database.SQLiteDatabase", loader)
        SQLiteCursorFactory = findClassIfExists(
                "$SQLiteDatabasePkg.database.SQLiteDatabase.CursorFactory", loader)
        SQLiteErrorHandler = findClassIfExists(
                "$SQLiteDatabasePkg.DatabaseErrorHandler", loader)
        SQLiteCancellationSignal = findClassIfExists(
                "$SQLiteDatabasePkg.support.CancellationSignal", loader)

        val pkgUI = "$WECHAT_PACKAGE_NAME.ui"
        MMActivity = findClassIfExists("$pkgUI.MMActivity", loader)
        MMFragmentActivity = findClassIfExists("$pkgUI.MMFragmentActivity", loader)
        MMListPopupWindow = findClassIfExists("$pkgUI.base.MMListPopupWindow", loader)

        PLTextView = findClassIfExists(
                "$WECHAT_PACKAGE_NAME.kiss.widget.textview.PLSysTextView", loader)
        RemittanceAdapter = findClassIfExists(
                "$WECHAT_PACKAGE_NAME.plugin.remittance.ui.RemittanceAdapterUI", loader)

        val pkgSnsUI = "$WECHAT_PACKAGE_NAME.plugin.sns.ui"
        SnsUploadUI = findClassesFromPackage(loader, classes, pkgSnsUI)
                .filterBySuper(MMActivity)
                .filterByField("$pkgSnsUI.SnsUploadSayFooter")
                .firstOrNull("SnsUploadUI")
        SnsUploadUIEditTextField = findFieldsWithType(
                SnsUploadUI, "$pkgSnsUI.SnsEditText"
        ).firstOrNull()?.name ?: ""
        AdFrameLayout = findClassIfExists("$pkgSnsUI.AdFrameLayout", loader)
        SnsPostTextView = findClassIfExists("$pkgSnsUI.widget.SnsPostDescPreloadTextView", loader)
        SnsPhotosContent = findClassIfExists("$pkgSnsUI.PhotosContent", loader)

        val pkgGalleryUI = "$WECHAT_PACKAGE_NAME.plugin.gallery.ui"
        AlbumPreviewUI = findClassIfExists("$pkgGalleryUI.AlbumPreviewUI", loader)
        SelectContactUI = findClassIfExists("$pkgUI.contact.SelectContactUI", loader)
        SelectConversationUI = findClassIfExists("$pkgUI.transmit.SelectConversationUI", loader)
        SelectConversationUIMaxLimitMethod = findMethodsByExactParameters(
                SelectConversationUI, C.Boolean, C.Boolean
        ).firstOrNull()?.name ?: ""

        val storageClasses = findClassesFromPackage(loader, classes, "$WECHAT_PACKAGE_NAME.storage")
        MsgInfoClass = storageClasses
                .filterByMethod(C.Boolean, "isSystem")
                .firstOrNull("MsgInfoClass")
        ContactInfoClass = storageClasses
                .filterByMethod(C.String, "getCityCode")
                .filterByMethod(C.String, "getCountryCode")
                .firstOrNull("ContactInfoClass")
        if (MsgInfoClass != null) {
            MsgStorageClass = storageClasses
                    .filterByMethod(C.Long, MsgInfoClass!!, C.Boolean)
                    .firstOrNull("MsgStorageClass")
            MsgStorageInsertMethod = findMethodsByExactParameters(
                    MsgStorageClass, C.Long, MsgInfoClass!!, C.Boolean
            ).firstOrNull()?.name ?: ""
        }

        val platformClasses = findClassesFromPackage(loader, classes,"$WECHAT_PACKAGE_NAME.sdk.platformtools")
        XMLParserClass = platformClasses
                .filterByMethod(C.Map, C.String, C.String)
                .firstOrNull("XMLParserClass")
        XMLParseMethod = findMethodsByExactParameters(
                XMLParserClass, C.Map, C.String, C.String
        ).firstOrNull()?.name ?: ""

//        ImgStorageClass = findClassesFromPackage(loader, classes, WECHAT_PACKAGE_NAME, 1)
//                .filterByMethod(C.String, ImgStorageLoadMethod, C.String, C.String, C.String, C.Boolean)
//                .firstOrNull("ImgStorageClass")
//        ImgStorageCacheField = findFieldsWithGenericType(
//                ImgStorageClass, "$CacheMapClass<java.lang.String, android.graphics.Bitmap>"
//        ).firstOrNull()?.name ?: ""
    }

    private fun getVersion(lpparam: XC_LoadPackage.LoadPackageParam): Version {
        val activityThreadClass = findClass("android.app.ActivityThread", null)
        val activityThread = callStaticMethod(activityThreadClass, "currentActivityThread")
        val context = callMethod(activityThread, "getSystemContext") as Context?
        val versionName = context?.packageManager?.getPackageInfo(lpparam.packageName, 0)?.versionName
        return Version(versionName ?: throw Error("Cannot get Wechat version"))
    }

    override fun toString(): String {
        return this.javaClass.declaredFields.filter {
            it.name != "INSTANCE"
        }.joinToString("\n") {
            it.isAccessible = true; "${it.name} = ${it.get(this)}"
        }
    }
}