package com.hyp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackDto extends BaseDto {

    private String mobileNumber;
    private String lastFeedbackAt;
    private String lastFeedback;
    private Integer hasBeenNotified;
}
