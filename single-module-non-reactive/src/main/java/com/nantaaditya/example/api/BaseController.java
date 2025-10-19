package com.nantaaditya.example.api;

import com.nantaaditya.example.helper.ObservationHelper;
import com.nantaaditya.example.helper.ObservationWrapper;
import com.nantaaditya.example.model.constant.ResponseCode;
import com.nantaaditya.example.model.error.GeneralFlowException;
import com.nantaaditya.example.model.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class BaseController {

  @Autowired
  private ObservationHelper observationHelper;

  @Autowired
  private ObservationWrapper observationWrapper;

  protected  <T> ResponseEntity<Response<T>> toResponse(Response<T> tResponse) {
    ResponseCode responseCode = ResponseCode.fromCode(tResponse.getResponse().getCode());

    HttpStatusCode httpStatusCode = ResponseCode.SUCCESS == responseCode ?
        HttpStatus.OK : HttpStatus.BAD_REQUEST;

    observationHelper.decorateErrorObservation(
        observationWrapper,
        new GeneralFlowException(responseCode),
        responseCode
    );

    return new ResponseEntity<>(tResponse, httpStatusCode);
  }
}
