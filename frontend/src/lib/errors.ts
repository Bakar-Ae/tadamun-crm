export function getLoadErrorMessage(resourceName: string) {
  return `We could not load ${resourceName}. Check your connection and try again.`
}

export function getSaveErrorMessage(resourceName: string) {
  return `We could not save ${resourceName}. Check the details and try again.`
}

export function getDeleteErrorMessage(resourceName: string) {
  return `We could not remove ${resourceName}. Please try again.`
}