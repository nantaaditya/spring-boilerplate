package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.ContextConstant;
import com.nantaaditya.example.model.constant.RetryConstant;
import com.nantaaditya.example.model.dto.AppLogMessage;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

@Log4j2
public class RetryHelper {

  private RetryHelper() {}

  public static <S, T, E extends Throwable> T execute(RetryTemplate retryTemplate, String processType,
      String processName, Function<S, T> action, Function<E, T> fallbackAction, S request) {
    return retryTemplate.execute(context -> execute(processType, processName, action, fallbackAction,
        request, context));
  }

  private static <S, T, E extends Throwable> T execute(String processType, String processName,
      Function<S, T> action, Function<E, T> fallbackAction, S request, RetryContext context) {
    T response = null;
    try {
      log.debug(AppLogMessage.create(String.format("#Retry - execute request [%s] [%s]", processType, processName), request));
      response = action.apply(request);
      return response;
    } catch (Throwable ex) {
      E genericException = (E) ex; //NOSONAR
      if (fallbackAction != null) {
        response = fallbackAction.apply(genericException);
      }

      updateRetryContext(processType, processName, context, request, response, ex);
      throw ex;
    }
  }

  private static <S, T> void updateRetryContext(String processType,
      String processName, RetryContext context, S request, T response, Throwable e) {
    context.setAttribute(RetryConstant.REQUEST.getName(), request);
    context.setAttribute(RetryConstant.REQUEST_ID.getName(), ContextHelper.get(ContextConstant.REQUEST_ID));
    context.setAttribute(RetryConstant.EXCEPTION.getName(), e.getCause());
    context.setAttribute(RetryConstant.PROCESS_TYPE.getName(), processType);
    context.setAttribute(RetryConstant.PROCESS_NAME.getName(), processName);
    if (response != null) {
      context.setAttribute(RetryConstant.RESPONSE.getName(), response);
    }
  }
}
