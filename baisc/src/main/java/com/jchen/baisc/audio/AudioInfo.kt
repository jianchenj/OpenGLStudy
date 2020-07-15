package com.jchen.baisc.audio

import android.media.AudioFormat

/**
 * sampleRate : 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
 *
 * channel : CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
 *
 * format :  采样位数，返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
 */
data class AudioInfo(
    var sampleRate: Int = 44100,
    var channel: Int = AudioFormat.CHANNEL_IN_MONO,
    var format: Int = AudioFormat.ENCODING_PCM_16BIT,
    var bitRate: Int = 96000
) {

}