import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Switch,
  ScrollView,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import NotificationModule from '../NativeNotificationModule';

const INTERVAL_OPTIONS = [1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180];
const HOURS = Array.from({ length: 24 }, (_, i) => i);
const MINUTES = Array.from({ length: 60 }, (_, i) => i);

interface TimeRange {
  startHour: number;
  startMinute: number;
  endHour: number;
  endMinute: number;
}

const DEFAULT_RANGE: TimeRange = {
  startHour: 8,
  startMinute: 0,
  endHour: 20,
  endMinute: 0,
};

const WheelPicker = ({
  items,
  value,
  onChange,
  disabled,
  compact = false,
}: {
  items: number[];
  value: number;
  onChange: (val: number) => void;
  disabled: boolean;
  compact?: boolean;
}) => {
  const currentIndex = items.indexOf(value);

  const prev = () => {
    if (disabled || currentIndex <= 0) return;
    onChange(items[currentIndex - 1]);
  };

  const next = () => {
    if (disabled || currentIndex >= items.length - 1) return;
    onChange(items[currentIndex + 1]);
  };

  return (
    <View style={[wStyles.container, compact && wStyles.containerCompact]}>
      <TouchableOpacity
        style={[wStyles.arrow, disabled && wStyles.arrowDisabled]}
        onPress={prev}
        disabled={disabled || currentIndex <= 0}
      >
        <Text
          style={[wStyles.arrowText, disabled && wStyles.arrowTextDisabled]}
        >
          ▲
        </Text>
      </TouchableOpacity>

      <View
        style={[
          wStyles.current,
          compact && wStyles.currentCompact,
          disabled && wStyles.currentDisabled,
        ]}
      >
        <Text
          style={[
            wStyles.currentText,
            compact && wStyles.currentTextCompact,
            disabled && wStyles.currentTextDisabled,
          ]}
        >
          {String(value).padStart(2, '0')}
        </Text>
      </View>

      <TouchableOpacity
        style={[wStyles.arrow, disabled && wStyles.arrowDisabled]}
        onPress={next}
        disabled={disabled || currentIndex >= items.length - 1}
      >
        <Text
          style={[wStyles.arrowText, disabled && wStyles.arrowTextDisabled]}
        >
          ▼
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const wStyles = StyleSheet.create({
  container: { alignItems: 'center' },
  containerCompact: { marginHorizontal: 2 },
  arrow: { padding: 4 },
  arrowDisabled: { opacity: 0.3 },
  arrowText: { fontSize: 14, color: '#1976D2' },
  arrowTextDisabled: { color: '#ccc' },
  current: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    marginVertical: 2,
    minWidth: 50,
    alignItems: 'center',
  },
  currentCompact: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    minWidth: 40,
  },
  currentDisabled: { backgroundColor: '#B0BEC5' },
  currentText: { fontSize: 22, fontWeight: 'bold', color: 'white' },
  currentTextCompact: { fontSize: 18 },
  currentTextDisabled: { color: '#E0E0E0' },
});

const HomeScreen: React.FC = () => {
  const [isEnabled, setIsEnabled] = useState(false);
  const [intervalMinutes, setIntervalMinutes] = useState(60);
  const [timeRanges, setTimeRanges] = useState<TimeRange[]>([DEFAULT_RANGE]);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const e = await AsyncStorage.getItem('enabled');
      const i = await AsyncStorage.getItem('interval');
      const tr = await AsyncStorage.getItem('timeRanges');
      if (e) setIsEnabled(JSON.parse(e));
      if (i) setIntervalMinutes(JSON.parse(i));
      if (tr) setTimeRanges(JSON.parse(tr));
    } catch (err) {
      console.log('Load error:', err);
    }
  };

  const saveSettings = async (
    en: boolean,
    int: number,
    ranges: TimeRange[],
  ) => {
    try {
      await AsyncStorage.setItem('enabled', JSON.stringify(en));
      await AsyncStorage.setItem('interval', JSON.stringify(int));
      await AsyncStorage.setItem('timeRanges', JSON.stringify(ranges));
    } catch (err) {
      console.log('Save error:', err);
    }
  };

  const start = async () => {
    try {
      const rangesJson = JSON.stringify(timeRanges);
      await NotificationModule.scheduleReminder(intervalMinutes, rangesJson);
      setIsEnabled(true);
      saveSettings(true, intervalMinutes, timeRanges);
      Alert.alert('已开启', `每${intervalMinutes}分钟提醒喝水`);
    } catch (e) {
      console.log('Start error:', e);
      Alert.alert('错误', '启动失败');
    }
  };

  const stop = async () => {
    try {
      await NotificationModule.cancelReminder();
      setIsEnabled(false);
      saveSettings(false, intervalMinutes, timeRanges);
    } catch (e) {
      console.log('Stop error:', e);
    }
  };

  const toggle = () => {
    if (isEnabled) {
      stop();
    } else {
      start();
    }
  };

  const testReminder = async () => {
    try {
      await NotificationModule.testReminder();
    } catch (e) {
      console.log('Test error:', e);
    }
  };

  const onIntervalChange = (val: number) => {
    setIntervalMinutes(val);
    saveSettings(isEnabled, val, timeRanges);
  };

  const addTimeRange = () => {
    const newRanges = [...timeRanges, { ...DEFAULT_RANGE }];
    setTimeRanges(newRanges);
    saveSettings(isEnabled, intervalMinutes, newRanges);
  };

  const removeTimeRange = (index: number) => {
    const newRanges = timeRanges.filter((_, i) => i !== index);
    setTimeRanges(newRanges);
    saveSettings(isEnabled, intervalMinutes, newRanges);
  };

  const onTimeChange = (
    index: number,
    field: keyof TimeRange,
    value: number,
  ) => {
    const newRanges = [...timeRanges];
    newRanges[index] = { ...newRanges[index], [field]: value };
    setTimeRanges(newRanges);
    saveSettings(isEnabled, intervalMinutes, newRanges);
  };

  const fmtTime = (h: number, m: number) =>
    `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: '#f5f5f5' }}
      contentContainerStyle={s.c}
    >
      <View style={s.h}>
        <Text style={s.t}>💧 兔兔喝水啦</Text>
        <Text style={s.st}>保持健康，定时喝水</Text>
      </View>

      <View style={s.card}>
        <Text style={s.ct}>提醒设置</Text>

        <View style={s.row}>
          <Text style={s.lb}>启用提醒</Text>
          <Switch
            trackColor={{ false: '#767577', true: '#81b0ff' }}
            thumbColor={isEnabled ? '#4CAF50' : '#f4f3f4'}
            onValueChange={toggle}
            value={isEnabled}
          />
        </View>

        <View style={s.div} />

        <View style={s.section}>
          <Text style={[s.lb, isEnabled && s.lbOff]}>提醒间隔</Text>
          <View style={s.pickerRow}>
            <WheelPicker
              items={INTERVAL_OPTIONS}
              value={intervalMinutes}
              onChange={onIntervalChange}
              disabled={isEnabled}
            />
            <Text style={[s.unitText, isEnabled && s.lbOff]}>分钟</Text>
          </View>
        </View>

        <View style={s.div} />

        <View style={s.section}>
          <View style={s.sectionHeader}>
            <Text style={[s.lb, isEnabled && s.lbOff]}>提醒时间段</Text>
            {!isEnabled && (
              <TouchableOpacity style={s.addBtn} onPress={addTimeRange}>
                <Text style={s.addBtnText}>+ 添加</Text>
              </TouchableOpacity>
            )}
          </View>

          {timeRanges.map((range, index) => (
            <View key={index} style={s.rangeCard}>
              <View style={s.rangeHeader}>
                <Text style={s.rangeTitle}>时间段 {index + 1}</Text>
                {!isEnabled && timeRanges.length > 1 && (
                  <TouchableOpacity onPress={() => removeTimeRange(index)}>
                    <Text style={s.removeBtn}>删除</Text>
                  </TouchableOpacity>
                )}
              </View>

              <View style={s.timeRowBlock}>
                <Text style={s.timeLabel}>开始</Text>
                <View style={s.timePickerRow}>
                  <WheelPicker
                    items={HOURS}
                    value={range.startHour}
                    onChange={v => onTimeChange(index, 'startHour', v)}
                    disabled={isEnabled}
                    compact
                  />
                  <Text style={[s.timeSep, isEnabled && s.lbOff]}>:</Text>
                  <WheelPicker
                    items={MINUTES}
                    value={range.startMinute}
                    onChange={v => onTimeChange(index, 'startMinute', v)}
                    disabled={isEnabled}
                    compact
                  />
                </View>
              </View>

              <View style={s.rangeArrowRow}>
                <View style={s.arrowLine} />
                <Text style={s.rangeArrow}>↓</Text>
                <View style={s.arrowLine} />
              </View>

              <View style={s.timeRowBlock}>
                <Text style={s.timeLabel}>结束</Text>
                <View style={s.timePickerRow}>
                  <WheelPicker
                    items={HOURS}
                    value={range.endHour}
                    onChange={v => onTimeChange(index, 'endHour', v)}
                    disabled={isEnabled}
                    compact
                  />
                  <Text style={[s.timeSep, isEnabled && s.lbOff]}>:</Text>
                  <WheelPicker
                    items={MINUTES}
                    value={range.endMinute}
                    onChange={v => onTimeChange(index, 'endMinute', v)}
                    disabled={isEnabled}
                    compact
                  />
                </View>
              </View>

              <Text style={s.rangeSummary}>
                {fmtTime(range.startHour, range.startMinute)} →{' '}
                {fmtTime(range.endHour, range.endMinute)}
              </Text>
            </View>
          ))}
        </View>

        {isEnabled && (
          <View style={s.lockBox}>
            <Text style={s.lockText}>🔒 请先关闭提醒以修改设置</Text>
          </View>
        )}
      </View>

      <View style={s.card}>
        <Text style={s.ct}>测试提醒</Text>
        <TouchableOpacity style={s.testBtn} onPress={testReminder}>
          <Text style={s.testBt}>🔔 测试提醒</Text>
        </TouchableOpacity>
      </View>

      <View style={s.ft}>
        <Text style={s.ftt}>
          提示：只在设定的时间段内提醒，锁屏也能语音提示
        </Text>
      </View>
    </ScrollView>
  );
};

const s = StyleSheet.create({
  c: {
    flexGrow: 1,
    backgroundColor: '#f5f5f5',
    padding: 20,
    paddingBottom: 40,
  },
  h: { alignItems: 'center', marginBottom: 25, marginTop: 15 },
  t: { fontSize: 30, fontWeight: 'bold', color: '#2196F3', marginBottom: 8 },
  st: { fontSize: 15, color: '#666' },
  card: {
    backgroundColor: 'white',
    borderRadius: 15,
    padding: 18,
    marginBottom: 15,
    elevation: 5,
  },
  ct: { fontSize: 17, fontWeight: '600', color: '#333', marginBottom: 12 },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  lb: { fontSize: 15, color: '#555', marginBottom: 10 },
  lbOff: { color: '#bbb' },
  div: { height: 1, backgroundColor: '#eee', marginVertical: 8 },
  section: { paddingVertical: 6 },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  addBtn: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  addBtnText: { color: 'white', fontSize: 13, fontWeight: '600' },
  pickerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  unitText: { fontSize: 15, color: '#555', marginLeft: 10 },
  rangeCard: {
    backgroundColor: '#F5F5F5',
    borderRadius: 10,
    padding: 12,
    marginBottom: 10,
  },
  rangeHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  rangeTitle: { fontSize: 14, fontWeight: '600', color: '#333' },
  removeBtn: { fontSize: 13, color: '#F44336' },
  timeRowBlock: {
    alignItems: 'center',
    marginVertical: 4,
  },
  timeLabel: { fontSize: 12, color: '#888', marginBottom: 4 },
  timePickerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  timeSep: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginHorizontal: 2,
  },
  rangeArrowRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 4,
  },
  arrowLine: {
    flex: 1,
    height: 1,
    backgroundColor: '#DDD',
    marginHorizontal: 10,
  },
  rangeArrow: { fontSize: 18, color: '#2196F3', marginHorizontal: 8 },
  rangeSummary: {
    fontSize: 14,
    color: '#2196F3',
    textAlign: 'center',
    marginTop: 8,
    fontWeight: '500',
  },
  lockBox: {
    backgroundColor: '#FFF3E0',
    padding: 10,
    borderRadius: 8,
    marginTop: 10,
    alignItems: 'center',
  },
  lockText: { fontSize: 13, color: '#E65100' },
  testBtn: {
    backgroundColor: '#FF9800',
    padding: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  testBt: { color: 'white', fontSize: 15, fontWeight: '600' },
  ft: {
    marginTop: 15,
    marginBottom: 30,
    padding: 12,
    backgroundColor: '#E3F2FD',
    borderRadius: 10,
  },
  ftt: { fontSize: 13, color: '#1976D2', textAlign: 'center' },
});

export default HomeScreen;
