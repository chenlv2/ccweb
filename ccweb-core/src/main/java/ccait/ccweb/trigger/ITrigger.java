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
import ccait.ccweb.model.QueryInfo;
import ccait.ccweb.model.ResponseData;
import org.springframework.core.annotation.Order;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ITrigger {

    @OnInsert
    @Order(-55555)
    void onInsert(Map<String, Object> data, HttpServletRequest request) throws Exception;

    @OnUpdate
    @Order(-55555)
    void onUpdate(Map<String, Object> data, HttpServletRequest request) throws Exception;

    @OnDelete
    @Order(-55555)
    void onDelete(String id, HttpServletRequest request) throws Exception;

    @OnList
    @Order(-55555)
    void onList(QueryInfo queryInfo, HttpServletRequest request) throws Exception;

    @OnView
    @Order(-55555)
    void onView(String id, HttpServletRequest request) throws Exception;

    @OnQuery
    @Order(-55555)
    void onQuery(QueryInfo queryInfo, HttpServletRequest request) throws Exception;

    @OnResponse
    @Order(-55555)
    void onResponse(HttpServletResponse response, HttpServletRequest request) throws Exception;

    @OnSuccess
    @Order(-55555)
    void onSuccess(ResponseData responseData, HttpServletRequest request) throws Exception;

    @OnError
    @Order(-55555)
    void onError(Exception ex, HttpServletRequest request);
}
