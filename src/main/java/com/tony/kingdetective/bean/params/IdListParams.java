package com.tony.kingdetective.bean.params;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * <p>
 * IdListParams
 * </p >
 *
 * @author Tony Wang
 * @since 2024/11/13 17:00
 */
@Data
public class IdListParams {

    @NotEmpty(message = "id????")
    private List<String> idList;
}
