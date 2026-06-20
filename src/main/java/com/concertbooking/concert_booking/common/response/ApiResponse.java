package com.concertbooking.concert_booking.common.response;
//Standard response wrapper mtlb sab reponses aise structured hoge

import com.concertbooking.concert_booking.booking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
     private boolean success;
     private String message;
     private T data;

     public static <T> ApiResponse<T> success(T data, String message){
         return ApiResponse.<T>builder()
                 .success(true)
                 .message(message)
                 .data(data)
                 .build();
     }
    public static <T> ApiResponse<T> success(T data){
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }
    public static <T> ApiResponse<T> error(String message){
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
