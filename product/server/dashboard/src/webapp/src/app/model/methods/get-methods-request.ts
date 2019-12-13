export class GetMethodsRequest {
    applications: string[];
    environments: string[];
    maxResults = 100;
    minCollectedDays = 30;
    normalizeSignature = true;
    onlyInvokedAfterMillis = 0;
    onlyInvokedBeforeMillis = Number.MAX_VALUE;
    signature = '';
    suppressUntrackedMethods = true;
}
