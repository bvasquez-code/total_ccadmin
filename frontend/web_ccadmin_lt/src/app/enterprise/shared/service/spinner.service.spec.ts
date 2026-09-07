import { SpinnerService } from './spinner.service';

describe('SpinnerService', () => {
  let service: SpinnerService;
  beforeEach(() => service = new SpinnerService());

  it('keeps the loader until all concurrent operations finish and replays the current state', () => {
    const states: boolean[] = [];
    service.show();
    const subscription = service.IsLoading$.subscribe(state => states.push(state));
    service.show();
    service.hide();
    expect(service.isLoading).toBeTrue();
    service.hide();
    expect(states).toEqual([true, false]);
    subscription.unsubscribe();
  });

  it('does not let ignored requests or extra hides consume a pending operation', () => {
    service.hide();
    service.show();
    service.show(false);
    service.hide(false);
    expect(service.isLoading).toBeTrue();
    service.hide();
    expect(service.isLoading).toBeFalse();
  });

  it('preserves another operation when a wrapped operation finishes', async () => {
    service.show();
    expect(await service.run(async () => 42)).toBe(42);
    expect(service.isLoading).toBeTrue();
    service.hide();
    expect(service.isLoading).toBeFalse();
  });

  it('releases the loader after asynchronous and synchronous failures', async () => {
    await expectAsync(service.run(async () => { throw new Error('Network error'); })).toBeRejected();
    expect(service.isLoading).toBeFalse();
    await expectAsync(service.run(() => { throw new Error('Setup error'); })).toBeRejected();
    expect(service.isLoading).toBeFalse();
  });
});
