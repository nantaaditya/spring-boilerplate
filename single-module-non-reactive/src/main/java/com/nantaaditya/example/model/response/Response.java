package com.nantaaditya.example.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nantaaditya.example.helper.ContextHelper;
import com.nantaaditya.example.helper.DateTimeHelper;
import com.nantaaditya.example.model.constant.ResponseCode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("java:S1068")
public class Response<T> {

  private ResponseMetadata response;
  private T data;
  private ErrorMetadata error;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @SuppressWarnings("java:S1068")
  public static class ResponseMetadata {
    private String code;
    private String description;
    private String time;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @SuppressWarnings("java:S1068")
  public static class ErrorMetadata {
    private Map<String, List<String>> violations;
  }

  public static <T> Response<T> success(T data) {
    ResponseMetadata responseMetadata = ResponseMetadata.builder()
        .code(ResponseCode.SUCCESS.getCode())
        .description(ResponseCode.SUCCESS.getMessage())
        .time(DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT))
        .build();

    ContextHelper.update(contextDTO -> contextDTO.withResponse(responseMetadata));

    return Response.<T>builder()
        .response(responseMetadata)
        .data(data)
        .build();
  }

  public static <T> Response<T> failed(ResponseCode responseCode, Map<String, List<String>> errors) {
    ResponseMetadata responseMetadata = ResponseMetadata.builder()
        .code(responseCode.getCode())
        .description(responseCode.getMessage())
        .time(DateTimeHelper.getDateInFormat(ZonedDateTime.now(), DateTimeHelper.ISO_8601_GMT7_FORMAT))
        .build();

    ContextHelper.update(contextDTO -> contextDTO.withResponse(responseMetadata));

    return Response.<T>builder()
        .response(responseMetadata)
        .error(ErrorMetadata.builder()
            .violations(errors)
            .build()
        )
        .build();
  }
}
