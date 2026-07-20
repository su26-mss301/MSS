package wardrobe.project.com.apigateway.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceApiResponse<T> {

    private boolean success;
    private T data;
}
