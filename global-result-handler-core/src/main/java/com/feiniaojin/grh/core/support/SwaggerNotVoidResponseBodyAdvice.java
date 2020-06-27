package com.feiniaojin.grh.core.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feiniaojin.grh.def.ResponseBean;
import com.feiniaojin.grh.def.ResponseBeanFactory;
import java.lang.reflect.Method;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 非空返回值的处理.
 *
 * @author <a href="mailto:943868899@qq.com">feiniaojin</a>
 * @version 0.1
 * @since 0.1
 */
@ControllerAdvice
@Order(value = 1000)
public class SwaggerNotVoidResponseBodyAdvice implements ResponseBodyAdvice<Object>,
    ApplicationContextAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerNotVoidResponseBodyAdvice.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static ApplicationContext applicationContext;

  @Resource
  private ResponseBeanFactory responseBeanFactory;


  /**
   * 只处理不返回void的，并且MappingJackson2HttpMessageConverter支持的类型.
   *
   * @param methodParameter 方法参数
   * @param clazz           处理器
   * @return 是否支持
   */
  @Override
  public boolean supports(MethodParameter methodParameter,
                          Class<? extends HttpMessageConverter<?>> clazz) {

    return !methodParameter.getMethod().getReturnType().equals(Void.TYPE)
        && MappingJackson2HttpMessageConverter.class.isAssignableFrom(clazz);
  }

  @Override
  public Object beforeBodyWrite(Object body,
                                MethodParameter methodParameter,
                                MediaType mediaType,
                                Class<? extends HttpMessageConverter<?>> clazz,
                                ServerHttpRequest serverHttpRequest,
                                ServerHttpResponse serverHttpResponse) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Warp for the return type is not void method!");
    }
    if (body == null) {
      return responseBeanFactory.newSuccessInstance();
    } else if (body instanceof ResponseBean
        || isSwagger(body, serverHttpRequest)) {
      return body;
    } else {
      return responseBeanFactory.newSuccessInstance(body);
    }
  }

  boolean isSwagger(Object body, ServerHttpRequest serverHttpRequest) {

    try {
      Object swaggerChecker = applicationContext.getBean(SwaggerChecker.class);
      if (swaggerChecker == null) {
        return false;
      }
      Class<?> swaggerCheckerClass = swaggerChecker.getClass();
      Method isSwaggerClass =
          swaggerCheckerClass.getDeclaredMethod("isSwaggerClass", Object.class);
      Boolean sc = (Boolean) isSwaggerClass.invoke(swaggerChecker, body);
      if (sc == true) {
        return true;
      }
      Method isSwaggerUrl =
          swaggerCheckerClass.getDeclaredMethod("isSwaggerUrl", ServerHttpRequest.class);
      Boolean su = (Boolean) isSwaggerUrl.invoke(swaggerChecker, serverHttpRequest);
      return su;
    } catch (Exception e) {
      return false;
    }
  }


  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    applicationContext = context;
  }
}
