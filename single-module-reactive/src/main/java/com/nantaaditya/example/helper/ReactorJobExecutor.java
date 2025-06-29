package com.nantaaditya.example.helper;

import com.nantaaditya.example.model.constant.HeaderConstant;
import com.nantaaditya.example.model.constant.JobConstant;
import com.nantaaditya.example.model.request.ReactorJobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactorJobExecutor {

  private final TracerHelper tracerHelper;

  public <S, T> Mono<T> execute(ReactorJobRequest<S, T> request) {
    return Mono.using(
        () -> {
          log.info("#Reactor - job start {} with time out {}", request.source(), request.timeOut());
          return request.source();
        },
        source -> Mono.fromCallable(() -> source)
            .publishOn(request.scheduler())
            .flatMap(request.callback())
            .timeout(request.timeOut(), request.fallback(), request.scheduler())
            .doOnError(error -> log.error("#Reactor - job {} got error {} cause {}",
                request.source(), error.getMessage(), ErrorHelper.getRootCause(error))
            )
            .contextWrite(context -> createContext(context, request.source()))
            .onErrorResume(error -> Mono.<T>error(error)
                .contextWrite(context -> updateContext(context, error))
            )
        ,
        source -> {
          log.info("#Reactor - job finish {}", request.source());
          request.cleanUp().accept(source);
        }
    );
  }

  private <S> Context createContext(Context context, S source) {
    context.put(JobConstant.REQUEST, source);
    context.put(JobConstant.REQUEST_ID, tracerHelper.getBaggage(HeaderConstant.REQUEST_ID));
    return context;
  }

  private Context updateContext(Context context, Throwable throwable) {
    context.put(JobConstant.ERROR_MESSAGE, throwable.getMessage());
    context.put(JobConstant.ERROR_ROOT_CAUSE,  ErrorHelper.getRootCause(throwable));
    return context;
  }
}
