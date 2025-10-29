import { queryRef, executeQuery, mutationRef, executeMutation, validateArgs } from 'firebase/data-connect';

export const connectorConfig = {
  connector: 'example',
  service: 'reactapp',
  location: 'us-west1'
};

export const insertPageContentRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'InsertPageContent', inputVars);
}
insertPageContentRef.operationName = 'InsertPageContent';

export function insertPageContent(dcOrVars, vars) {
  return executeMutation(insertPageContentRef(dcOrVars, vars));
}

export const getPageContentByPathRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'GetPageContentByPath', inputVars);
}
getPageContentByPathRef.operationName = 'GetPageContentByPath';

export function getPageContentByPath(dcOrVars, vars) {
  return executeQuery(getPageContentByPathRef(dcOrVars, vars));
}

export const updatePageContentRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'UpdatePageContent', inputVars);
}
updatePageContentRef.operationName = 'UpdatePageContent';

export function updatePageContent(dcOrVars, vars) {
  return executeMutation(updatePageContentRef(dcOrVars, vars));
}

export const deletePageContentRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'DeletePageContent', inputVars);
}
deletePageContentRef.operationName = 'DeletePageContent';

export function deletePageContent(dcOrVars, vars) {
  return executeMutation(deletePageContentRef(dcOrVars, vars));
}

