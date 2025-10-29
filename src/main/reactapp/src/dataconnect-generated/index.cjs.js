const { queryRef, executeQuery, mutationRef, executeMutation, validateArgs } = require('firebase/data-connect');

const connectorConfig = {
  connector: 'example',
  service: 'reactapp',
  location: 'us-west1'
};
exports.connectorConfig = connectorConfig;

const insertPageContentRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'InsertPageContent', inputVars);
}
insertPageContentRef.operationName = 'InsertPageContent';
exports.insertPageContentRef = insertPageContentRef;

exports.insertPageContent = function insertPageContent(dcOrVars, vars) {
  return executeMutation(insertPageContentRef(dcOrVars, vars));
};

const getPageContentByPathRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'GetPageContentByPath', inputVars);
}
getPageContentByPathRef.operationName = 'GetPageContentByPath';
exports.getPageContentByPathRef = getPageContentByPathRef;

exports.getPageContentByPath = function getPageContentByPath(dcOrVars, vars) {
  return executeQuery(getPageContentByPathRef(dcOrVars, vars));
};

const updatePageContentRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'UpdatePageContent', inputVars);
}
updatePageContentRef.operationName = 'UpdatePageContent';
exports.updatePageContentRef = updatePageContentRef;

exports.updatePageContent = function updatePageContent(dcOrVars, vars) {
  return executeMutation(updatePageContentRef(dcOrVars, vars));
};

const deletePageContentRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'DeletePageContent', inputVars);
}
deletePageContentRef.operationName = 'DeletePageContent';
exports.deletePageContentRef = deletePageContentRef;

exports.deletePageContent = function deletePageContent(dcOrVars, vars) {
  return executeMutation(deletePageContentRef(dcOrVars, vars));
};
