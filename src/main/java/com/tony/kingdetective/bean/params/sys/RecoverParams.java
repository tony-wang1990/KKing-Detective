package com.tony.kingdetective.bean.params.sys;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * <p>
 * RecoverParams
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/12/2 14:58
 */
@Data
public class RecoverParams {

    @Size(min = 1, max = 1, message = "??????1")
    @NotEmpty(message = "????????")
    private List<MultipartFile> fileList;

    private String encryptionKey;
}
