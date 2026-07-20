package wardrobe.project.com.apigateway.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivityPoint {

    private String date;
    private String dayLabel;
    private long users;
    private long detections;
    private long recommendations;
}
