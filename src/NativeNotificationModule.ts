import { TurboModule, TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  scheduleReminder(intervalMinutes: number, timeRangesJson: string): Promise<boolean>;
  cancelReminder(): Promise<boolean>;
  testReminder(): Promise<boolean>;
  showNotification(title: string, message: string): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NotificationModule');
