/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.netflix.spinnaker.orca.retrofit.exceptions

import com.netflix.spinnaker.orca.batch.exceptions.ExceptionHandler
import retrofit.RetrofitError
import retrofit.mime.TypedByteArray

class RetrofitExceptionHandler implements ExceptionHandler<RetrofitError> {
  @Override
  boolean handles(Exception e) {
    return e.class == RetrofitError
  }

  @Override
  ExceptionHandler.Response handle(String stepName, RetrofitError e) {
    e.getResponse().with {
      def response = new ExceptionHandler.Response(exceptionType: e.class.simpleName, operation: stepName)

      try {
        def body = e.getBodyAs(Map) as Map

        def error = body.error ?: reason
        def errors = body.errors ?: (body.messages ?: []) as List<String>
        errors = errors ?: (body.message ? [body.message] : [])

        response.details = new ExceptionHandler.ResponseDetails(error, errors as List<String>)

        if (body.exception) {
          response.details.rootException = body.exception
        }
      } catch (ignored) {
        response.details = new ExceptionHandler.ResponseDetails(properties.reason ?: e.message)
      }

      try {
        response.details.responseBody = new String(((TypedByteArray) e.getResponse().getBody()).getBytes())
      } catch (ignored) {
        response.details.responseBody = null
      }
      response.details.kind = e.kind
      response.details.status = properties.status ?: null
      response.details.url = properties.url ?: null
      response.shouldRetry = (e.kind == RetrofitError.Kind.NETWORK)
      return response
    }
  }
}
