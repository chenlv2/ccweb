/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.trigger;

import ccait.ccweb.annotation.*;
import ccait.ccweb.controllers.BaseController;
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.ResponseData;
import org.springframework.core.annotation.Order;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ITrigger {

    /***
     * 新增数据事件
     * @param list （提交的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnInsert
    @Order(-55555)
    void onInsert(List<Map<String, Object>> list, HttpServletRequest request) throws Exception;

    /***
     * 更新数据事件
     * @param queryInfo （提交的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnUpdate
    @Order(-55555)
    void onUpdate(QueryInfo queryInfo, HttpServletRequest request) throws Exception;

    /***
     * 删除数据事件
     * @param id （要删除的数据ID）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnDelete
    @Order(-55555)
    void onDelete(String id, HttpServletRequest request) throws Exception;

    /***
     * 列出数据事件，当queryInfo没有查询条件时触发
     * @param queryInfo （分页/分组/排序条件）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnList
    @Order(-55555)
    void onList(QueryInfo queryInfo, HttpServletRequest request) throws Exception;

    /***
     * 浏览数据事件，ID查询时触发
     * @param id （要浏览的数据ID）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnView
    @Order(-55555)
    void onView(String id, HttpServletRequest request) throws Exception;

    /***
     * 查询数据事件，queryInfo存在查询条件时触发
     * @param queryInfo （查询/分页/分组/排序条件）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnQuery
    @Order(-55555)
    void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception;

    /***
     * 响应数据流时触发
     * @param response （响应对象）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnResponse
    @Order(-55555)
    void onResponse(HttpServletResponse response, HttpServletRequest request) throws Exception;

    /***
     * 成功返回数据时触发
     * @param responseData （响应的数据）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnSuccess
    @Order(-55555)
    void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception;

    /***
     * 返回错误数据时触发
     * @param ex （Exception异常类）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnError
    @Order(-55555)
    void onError(Exception ex, HttpServletRequest request) throws Exception;

    /***
     * 上传文件时触发
     * @param data （文件字节数组）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnUpload
    @Order(-55555)
    void onUpload(byte[] data, HttpServletRequest request) throws Exception;

    /***
     * 下载文件时触发
     * @param data （文件对象）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnDownload
    @Order(-55555)
    void onDownload(BaseController.DownloadData data, HttpServletRequest request) throws Exception;

    /***
     * 预览文档时触发
     * @param data （文件对象）
     * @param request （当前请求）
     * @throws Exception
     */
    @OnPreviewDoc
    @Order(-55555)
    void onPreviewDoc(BaseController.DownloadData data, HttpServletRequest request) throws Exception;
}
