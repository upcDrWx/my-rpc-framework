package github.wx;

import lombok.*;

import java.io.Serializable;

/**
 * @author wx
 * @date 2023/10/5 16:17
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Hello implements Serializable {
    private String message;
    private String description;
}
