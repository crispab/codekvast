/**
 * The client side state for the app.
 */

const oneHourInMillis = 60 * 60 * 1000;
const oneDayInMillis = 24 * oneHourInMillis;

export class ClientSettings {
    static KEY = 'settings';
    readonly sampleTimestamp = new Date().getTime() - 37 * oneDayInMillis - 13 * oneHourInMillis;

    dateFormat = 'age';
    advancedControls = false;
}

