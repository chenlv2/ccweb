/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/ccweb
 *  Note: to build on java, include the jdk1.8+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2019-02-10
 */


package ccait.ccweb.resolver;


import ccait.ccweb.filter.CCWebRequestWrapper;
import ccait.ccweb.utils.FastJsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Configuration
public class RequestArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LogManager.getLogger( RequestArgumentResolver.class );

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        final String parameterJson = ((CCWebRequestWrapper) ((ServletWebRequest) webRequest).getRequest()).getRequestPostString();
        final Type type = parameter.getGenericParameterType();
        Class clazz = null;

        if(type.getTypeName().indexOf("List<") > 0) {
            List list = FastJsonUtils.convertJsonToObject(parameterJson, List.class);
            List result = new ArrayList();
            for(int i=0; i<list.size();i++) {
                Type argType = ((ParameterizedTypeImpl) type).getActualTypeArguments()[0];
                Object obj = FastJsonUtils.convert(list.get(i), getClassByType(argType) );
                result.add(obj);
            }

            return result;
        }

        else {

            clazz = getClassByType(type);
        }


        return FastJsonUtils.convertJsonToObject(parameterJson, clazz);
    }

    public Class getClassByType(Type type) {
        Class clazz;
        if(type.getTypeName().indexOf("Map<") > 0) {
            clazz = Map.class;
        }
        else {
            clazz = (Class) type;
        }
        return clazz;
    }
}
