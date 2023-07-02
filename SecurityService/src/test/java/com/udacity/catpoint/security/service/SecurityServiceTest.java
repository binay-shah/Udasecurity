package com.udacity.catpoint.security.service;

import com.udacity.catpoint.ImageService.FakeImageService;
import com.udacity.catpoint.ImageService.ImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityServiceTest {

    private SecurityService securityService;
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository, imageService);
    }

    @Test
    public void alarmArmedAndSensorActivated_putSystemPendingStatus(){
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.PENDING_ALARM);
    }

    @Test
    public void alarmArmedAndSensorActivatedAndSystemPending_setAlarmStatustoAlarm(){
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);
    }

    @Test
    public void pendingAlarmAndSensorsInActive_returnToAlarmState(){
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void alarmActive_changeSensorState_alarmStatusNoEffect(boolean isActive){
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        sensor.setActive(isActive);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);
    }

    @Test
    public void sensorActivatedAndAlreadyActiveAndSystemInPendingState_alarmStatusAlarm(){
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void sensorDeactivatedAndAlreadyInactive_alarmStatusNoChange(AlarmStatus alarmStatus) {
        Sensor sensor = new Sensor("testSensor", SensorType.DOOR);
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, false);
        Assertions.assertEquals(securityService.getAlarmStatus(), alarmStatus);
    }

    @Test
    public void imageAnalyzed_catDetectedAndStatusArmedHome_alarmStatusAlarm() {
        BufferedImage bufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);
    }

    @Test
    public void imageAnalyzed_catNotDetectedAndSensorsNotActive_alarmStatusNo() {
        when(securityRepository.getSensors()).thenReturn(
                Set.of(new Sensor[]{new Sensor("0", SensorType.DOOR),
                        new Sensor("1", SensorType.WINDOW),
                        new Sensor("2", SensorType.MOTION)}));
        BufferedImage bufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).thenReturn(false);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.NO_ALARM);
    }

    @Test
    public void systemDisarmed_alarmStatusNo() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void systemArmed_resetAllSensorsToInactive() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);

        when(securityRepository.getSensors()).thenReturn(
                Set.of(new Sensor[]{new Sensor("0", SensorType.DOOR),
                        new Sensor("1", SensorType.WINDOW),
                        new Sensor("2", SensorType.MOTION)}));
        Set<Sensor> sensors = securityService.getSensors();
        sensors.forEach((s) -> securityService.changeSensorActivationStatus(s, false));
        sensors.forEach((s) -> verify(securityRepository).updateSensor(s));
    }

    @Test
    public void cameraAnalyzed_CatAtHomeAndArmedHome_setStatusToAlarm() {
        BufferedImage bufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(bufferedImage, 50.0f)).thenReturn(true);

        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Assertions.assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);
    }



}
