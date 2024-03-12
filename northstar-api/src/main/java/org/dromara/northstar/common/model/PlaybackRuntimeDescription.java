package org.dromara.northstar.common.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回放状态
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackRuntimeDescription {

	private String gatewayId;
	
	private LocalDateTime playbackTimeState;
}
