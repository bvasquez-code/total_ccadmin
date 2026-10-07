export interface PinpadSignedMessageDto {
  payload: string;
  signature: string;
}

export interface PinpadBrowserInstructionsDto {
  loginUrl: string;
  registerUrl: string;
  statusUrl: string;
  ackUrl: string;
  loginCommand: PinpadSignedMessageDto;
  waitSeconds: number;
  pollMillis: number;
}
