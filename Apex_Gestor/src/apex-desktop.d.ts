export {};

declare global {
  interface Window {
    apexDesktop?: {
      defaultApiUrl?: string;
      isDesktop?: boolean;
      platform?: string;
      getDeviceInfo?: () => Promise<{
        defaultApiUrl: string;
        deviceFingerprint: string;
        deviceLabel: string;
        isDesktop: boolean;
        platform: string;
      }>;
      secureStorage?: {
        getItem: (key: string) => Promise<string | null>;
        setItem: (key: string, value: string) => Promise<boolean>;
        removeItem: (key: string) => Promise<boolean>;
      };
    };
  }
}
