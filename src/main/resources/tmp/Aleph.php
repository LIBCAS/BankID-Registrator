<?php

defined('BASEPATH') or exit('No direct script access allowed');

class Aleph
{
    public function getMyProfileX($user, $params = null)
    {
        $recordList = [];

        $params['loans'] = isset($params['loans']) ? $params['loans'] : 'N';
        $params['cash'] = isset($params['cash']) ? $params['cash'] : 'N';
        $params['hold'] = isset($params['hold']) ? $params['hold'] : 'N';

        $xml = $this->doXRequestUsingPost(
            'bor-info',
            [
                'loans' => $params['loans'], 'cash' => $params['cash'], 'hold' => $params['hold'],
                'library' => 'KNA50', 'bor_id' => $user, 'format' => '1'
            ],
            true
        );

        if (!empty($xml['error'])) {
            $recordList['error'] = $xml['error'];
            return $recordList;
        }

        $id = (string) $xml->z303->{'z303-id'};
        $group = (string) $xml->z305->{'z305-bor-status'};
        $credit_sum = (string) $xml->z305->{'z305-sum'};
        $credit_sign = (string) $xml->z305->{'z305-credit-debit'};
        $fullname = (string) $xml->z303->{'z303-name'};
        $firstname = (string) $xml->z303->{'z303-first-name'};
        $lastname = (string) $xml->z303->{'z303-last-name'};
        $con_lng = (string) $xml->z303->{'z303-con-lng'};
        $birth_date = (string) $xml->z303->{'z303-birth-date'};
        $export_consent = (string) $xml->z303->{'z303-export-consent'};
        $home_library = (string) $xml->z303->{'z303-home-library'};
        $password = '';

        foreach ($xml->z308 as $z308) {
            $z308_key_type = (string) $z308->{'z308-key-type'};

            if ($z308_key_type == "01") {
                $barcode = (string) $z308->{'z308-key-data'};
                $password = (string) $z308->{'z308-verification'};
            }

            if ($z308_key_type == "03") {
                $rfid = (string) $z308->{'z308-key-data'};
            }

            if ($z308_key_type == "05") {
                $card_number = (string) $z308->{'z308-key-data'};
            }
        }

        foreach ($xml->z304 as $z304) {
            $z304_seq = (string) $z304->{'z304-sequence'};
            if ($z304_seq == "01") {
                $ident_address1 = (string) $z304->{'z304-address-1'};
                $ident_address2 = (string) $z304->{'z304-address-2'};
                $ident_zip = (string) $z304->{'z304-zip'};
                $ident_sms_number = (string) $z304->{'z304-sms-number'};
                $ident_email = (string) $z304->{'z304-email-address'};
            }

            if ($z304_seq == "02") {
                $contact_address1 = (string) $z304->{'z304-address-1'};
                $contact_address2 = (string) $z304->{'z304-address-2'};
                $contact_zip = (string) $z304->{'z304-zip'};
                $contact_sms_number = (string) $z304->{'z304-sms-number'};
            }
        }

        if (empty($firstname) && strstr($fullname, ' ')) {
            // funguje pro Karel Hynek Mácha, nevhodné pro Vincent Van Gogh
            // TODO: jsou ta data jinde?
            $names = explode(' ', $fullname);
            $lastname = $names[0];
            array_shift($names);
            $firstname = implode(' ', $names);
        } else {
            $lastname = $fullname;
            $firstname = '';
        }

        if ($credit_sign == null) {
            $credit_sign = "C";
        }

        $xml_rest = $this->doRestDLFRequest(
            ['patron', $id, 'patronStatus', 'registration']
        );

        $xml_auth = $this->doXRequestUsingPost(
            'bor-auth',
            [
                'library' => 'KNA50',
                'bor_id' => $barcode,
                'verification' => $password,
                'sub_library' => 'KNAV',
                'lang' => 'cze'
            ],
            true
        );

        $expiry_date = (string) $xml_auth->z305->{'z305-expiry-date'};

        $user_type = (string) $xml_rest->registration->institution->{'z305-bor-status-code'};

        $recordList['firstname'] = $firstname;
        $recordList['lastname'] = $lastname;
        $recordList['ident_address1'] = !empty($ident_address1) ? $ident_address1 : '';
        $recordList['ident_address2'] = !empty($ident_address2) ? $ident_address2 : '';
        $recordList['ident_zip'] = !empty($ident_zip) ? $ident_zip : '';
        $recordList['sms_number'] = !empty($ident_sms_number) ? $ident_sms_number : '';
        $recordList['birth_date'] = date_format(date_create_from_format('d/m/Y', $birth_date), 'Y-m-d');
        $recordList['birth_date_text'] = $birth_date;
        $recordList['contact_address1'] = isset($contact_address1) ? $contact_address1 : null;
        $recordList['contact_address2'] = isset($contact_address2) ? $contact_address2 : null;
        $recordList['contact_zip'] = isset($contact_zip) ? $contact_zip : null;
        $recordList['contact']['sms_number'] = isset($contact_sms_number) ? $contact_sms_number : null;
        $recordList['con_lng'] = $con_lng;
        $recordList['email'] = isset($ident_email) ? $ident_email : null;
        $recordList['group'] = $group;
        $recordList['credit_sum'] = $credit_sum;
        $recordList['barcode'] = $barcode;
        $recordList['card_number'] = isset($card_number) ? $card_number : null;
        $recordList['rfid'] = isset($rfid) ? $rfid : null;
        $recordList['export_consent'] = $export_consent;
        $recordList['id'] = $id;
        $recordList['password'] = $password;
        $recordList['user_type'] = $user_type;
        $recordList['home_library'] = $home_library;
        $recordList['expiry_date'] = $expiry_date;

        return $recordList;
    }

    public function newPatron($params, $libraries = [])
    {
        // vytvori uzivatele
        // prideli mu podknihovny
        // kvůli vytvoření poplatku za registraci se tvoří falešná jednotka a požadavek na ni
        // požadavek i jednotka se vzápětí opět smažou, vzniklý poplatek zůstává
        $result = false;

        $CI = &get_instance();
        $sysno = $CI->config->item('sysno', 'aleph');

        // uživatelé s pouze vzdáleným přístupem nemají právo půjčovat si jednotky,
        // proto se vytvoří jako běžný uživatel - 16
        // a po vytvoření výpůjčky se změní na požadovaný typ
        if (in_array($params['status'], ['24', '97'])) {
            $orig_status = $params['status'];
            $params['status'] = '16';    // set to regular user
        }

        // uživatelé s pouze vzdáleným přístupem nemají právo půjčovat si jednotky,
        // proto se vytvoří jako běžný uživatel na tři roky - 23
        // a po vytvoření výpůjčky se změní na požadovaný typ
        if (in_array($params['status'], ['96'])) {
            $orig_status = $params['status'];
            $params['status'] = '23';
        }

        $patronXml = $this->createPatronXml($params);

        $result['create'] = $this->updateBorX($patronXml);
        $result['create']['patronxml'] = $patronXml;

        if (!empty($result['create']['error'])) {
            return $result['create'];
        }

        foreach ($libraries as $library) {
            $libraryXml = $this->createLibraryXml($params, $library);
            $result['sublibrary'][] = $this->updateBorX($libraryXml);
        }

        // rezervace jednotky jako způsob vytvoření platby
        if (!empty($params['user_status_fake'])) {
            $item_barcode = "2611" . $sysno . date("dHis");
            $result['item_barcode'] = $item_barcode;

            $description = 'Registrace';
            $itemXml = $this->createItemXml([
                'description' => $description,
                'sysno' => $sysno,
                'barcode' => $item_barcode,
                'type' => $params['user_status_fake']
            ]);
            $result['registration'] = $this->createAndBook($params['id'], $itemXml, $sysno, $description);
        }

        // změna vzdálených uživatelů na požadovaný typ
        if (!empty($orig_status)) {
            if (in_array($orig_status, ['24', '96', '97'])) {
                $update_xml = $this->createUpdateXmlForRemoteUsers($params['id'], $orig_status);
                $result['user_magic'] = $this->updateBorX($update_xml);
            }
        }

        return $result;
    }

    public function updatePatron($params)
    {
        $patronXml = $this->updatePatronXml($params);
        $result['update'] = $this->updateBorX($patronXml);

        if (!empty($result['update']->error)) {
            if ((strpos($result['update']->error, 'Login record belongs to another user') !== false) || (strpos($result['update']->error, 'Match found for ID') !== false) || (strpos($result['update']->error, 'Failed to generate new User ID') !== false)) {
                $result['error'] = "insert_fail_login_data";
            } elseif ($result['update']->error && (strpos($result['update']->error, 'Can not ins/upd record') !== false)) {
                $result['error'] = "insert_fail_z30x";
            } elseif (strpos($result['update']->error, 'Error retrieving Patron System Key') !== false) {
                $result['error'] = "patron_not_found";
            }
        }

        return $result;
    }

    public function changePassword($id, $barcode, $password)
    {
        try {
            $result['update'] = $this->updateBorX(
                $this->createPasswordXml($id, $barcode, $password)
            );
        } catch (Exception $e) {
            return ['error' => $e->getMessage()];
        }

        if (!empty($result['update']->error)) {
            if ((strpos($result['update']->error, 'Login record belongs to another user') !== false) || (strpos($result['update']->error, 'Match found for ID') !== false) || (strpos($result['update']->error, 'Failed to generate new User ID') !== false)) {
                $result['error'] = "insert_fail_login_data";
            } elseif ($result['update']->error && (strpos($result['update']->error, 'Can not ins/upd record') !== false)) {
                $result['error'] = "insert_fail_z30x";
            } elseif (strpos($result['update']->error, 'Error retrieving Patron System Key') !== false) {
                $result['error'] = "patron_not_found";
            }
        }

        return $result;
    }


    protected function updateBorX($xml)
    {
        return $this->doXRequestUsingPost(
            'update-bor',
            [
                'library' => 'KNA50',
                'update_flag' => 'Y',
                'xml_full_req' => $xml,
            ],
            true
        );
    }


    public function getBorInfo($id)
    {
        // cash Y, N, B - balance
        // loans Y, N, P - only z13 + tags
        // filter_cash
        // hold Y, N, P - short bibliographic information
        // format=1  retrieve all Z304 (Addresses) and z308 (Patron ID) records
        $params = array(
            'library' => 'KNA50',
            'bor_id' => $id,
            'cash' => 'N',
            'loans' => 'N',
            'hold' => 'N',
            'format' => '1'
        );

        try {
            return $this->doXRequestUsingPost('bor-info', $params, true);
        } catch (Exception $ex) {
            throw new Exception($ex->getMessage());
            return null;
        }
    }

    function getBorAuth($id, $password)
    {
        $params = array('library' => 'KNA50', 'bor_id' => $id, 'verification' => $password, 'sub_library' => 'KNAV', 'lang' => 'cze');

        try {
            return $this->doXRequestUsingPost('bor-auth', $params, true);
        } catch (Exception $ex) {
            throw new Exception($ex->getMessage());
            return null;
        }
    }

    public function paymentProcess($patronid, $fines_sum)
    {
        // zaplatit poplatek
        $result['payment'] = $this->payFine($patronid, $fines_sum);

        return $result;
    }


    // $result [reply-text] => ok
    public function payFine($patronid, $fines_sum)
    {
        $result = '';

        $post_xml = "post_xml=<pay-cash-parameters> <sum>" . $fines_sum . "</sum> <pay-reference>registrace na místě - ext.app</pay-reference></pay-cash-parameters>";

        try {
            $result = $this->doRestDLFRequest(['patron', $patronid, 'circulationActions', 'cash'], ["institution" => "KNA50"], "PUT", $post_xml);
        } catch (Exception $ex) {
            throw new \Exception("XServer error: $ex->getMessage()");
        }

        return $result;
    }


    // vytvoří falešnou jednotku a požadavek na ni a poté obojí opět smaže
    protected function createAndBook($patron, $xml_item, $docnum, $description)
    {
        $adm_library = "KNA50";

        $update = $this->createItem($xml_item, $docnum);

        $id_part = (string) $update->z30->{'z30-doc-number'};
        $item_seq = (string) $update->z30->{'z30-item-sequence'};
        $item_barcode = (string) $update->z30->{'z30-barcode'};

        // zarovnani na potrebnou delku id
        $id = str_pad($id_part, 9, "0", STR_PAD_LEFT);
        $item_seq = str_pad($item_seq, 6, "0", STR_PAD_LEFT);
        $itemid =  $adm_library . $id . $item_seq;
        sleep(1);

        $requiredBy = date("Ymd", strtotime("+1 month"));

        $details = array(
            "id" => $id,
            "item_id" => $itemid,
            "patron" => $patron,
            "pickUpLocation" => "KNAV",
            "comment" => $description,
            "requiredBy" => $requiredBy
        );

        $result['hold'] = $this->placeHold($details);
        $result['cancelRequest'] = $this->cancelRequest($id, $item_seq, "0001");
        $result['deleteItem'] = $this->deleteItem($id, $item_seq, $item_barcode);

        $result['id'] = $id;
        $result['item_seq'] = $item_seq;
        $result['item_barcode'] = $item_barcode;

        return $result;
    }


    protected function createItem($xml_item, $docnum)
    {
        $bib_library = "KNA01";

        $params = array('Xml_Full_Req' => $xml_item, 'Adm_Library' => 'KNA50', 'Adm_Doc_Number' => $docnum, 'Bib_Library' => $bib_library, 'Bib_Doc_Number' => $docnum);

        try {
            return $this->doXRequestUsingPost('create-item', $params, true);
        } catch (Exception $ex) {
            throw new \Exception($ex->getMessage());
            return null;
        }
    }


    protected function placeHold($details)
    {
        list($bib, $sys_no) = ['KNA01', $details['id']];
        $recordId = $bib . $sys_no;
        $itemId = $details['item_id'];
        $patron = $details['patron'];
        $pickupLocation = $details['pickUpLocation'];

        $comment = $details['comment'];
        if (strlen($comment) <= 50) {
            $comment1 = $comment;
        } else {
            $comment1 = substr($comment, 0, 50);
            $comment2 = substr($comment, 50, 50);
        }

        $requiredBy = $details['requiredBy'];

        $patronId = $patron;
        $body = new \SimpleXMLElement(
            '<?xml version="1.0" encoding="UTF-8"?>'
                . '<hold-request-parameters></hold-request-parameters>'
        );
        $body->addChild('pickup-location', $pickupLocation);
        $body->addChild('last-interest-date', $requiredBy);
        $body->addChild('note-1', $comment1);
        if (isset($comment2)) {
            $body->addChild('note-2', $comment2);
        }
        $body = 'post_xml=' . $body->asXML();
        try {
            $this->doRestDLFRequest(
                [
                    'patron', $patronId, 'record', $recordId, 'items', $itemId,
                    'hold'
                ],
                null,
                "PUT",
                $body
            );
        } catch (Exception $exception) {
            $message = $exception->getMessage();
            return [
                'success' => false,
                'sysMessage' => "$message"
            ];
            dump($message);
        }

        return ['success' => true];
    }

    protected function cancelRequest($sysno, $item_sequence, $sequence)
    {
        $params = array(
            'library' => 'kna50',
            'doc_number' => $sysno,
            'item_sequence' => $item_sequence,
            'sequence' => $sequence
        );
        $result = array();
        try {
            return $this->doXRequestUsingPost('hold-req-cancel', $params, true);
        } catch (Exception $ex) {
            dump($ex->getMessage());
        }

        return $result;
    }


    protected function deleteItem($doc_number, $item_sequence, $item_barcode)
    {
        $adm_library = "KNA50";

        $params = array('library' => $adm_library, 'doc_number' => $doc_number, 'item_sequence' => $item_sequence, 'item_barcode' => $item_barcode);

        try {
            $result = $this->doXRequestUsingPost('delete-item', $params, true);
        } catch (Exception $ex) {
            dump(array('success' => false, 'sysMessage' => $ex->getMessage()));
        }
        return $result;
    }

    public function getFines($id)
    {
        // cash Y, N, B - balance
        $params = array('library' => 'KNA50', 'bor_id' => $id, 'cash' => 'B', 'loans' => 'N', 'hold' => 'N', 'format' => '0', 'lang' => 'cze');

        try {
            return $this->doXRequestUsingPost('bor-info', $params, true);
        } catch (Exception $ex) {
            throw new Exception($ex->getMessage());
        }

        return null;
    }


    public function doXRequestUsingPost($op, $params)
    {
        $CI = &get_instance();

        $url = $CI->config->item('host', 'aleph') . ":" . $CI->config->item('port', 'aleph') . "/X?";
        $body = '';
        $sep = '';
        $params['op'] = $op;
        $params['user_name'] = $CI->config->item('wwwuser', 'aleph');
        $params['user_password'] = $CI->config->item('wwwpasswd', 'aleph');

        foreach ($params as $key => $value) {
            $body .= $sep . $key . '=' . urlencode($value);
            $sep = '&';
        }
        $result = $this->doHTTPRequest($url, 'POST', $body);

        if ($result->error && (strpos($result->error, 'Login record belongs to another user') !== false) || (strpos($result->error, 'Match found for ID') !== false) || (strpos($result->error, 'Failed to generate new User ID') !== false)) {
            $result['error'] = "insert_fail_login_data";
        } elseif ($result->error && (strpos($result->error, 'Can not ins/upd record') !== false)) {
            $result['error'] = "insert_fail_z30x";
        } elseif ($result->error && (strpos($result->error, 'Error retrieving Patron System Key') !== false)) {
            $result['error'] = "patron_not_found";
        } elseif ($result->error && (strpos($result->error, 'Error retrieving Local Patron Record') !== false)) {
            $result['error'] = "local_patron_not_found";
        } elseif ($result->error && (strpos($result->error, 'Succeeded') === false) && (strpos($result->error, 'success') === false)) {
            throw new Exception("XServer error (doXRequestUsingPost):" . $result->error);
        }

        return $result;
    }

    protected function doRestDLFRequest($path_elements, $params = null, $method = 'GET', $body = null)
    {
        $CI = &get_instance();

        $path = '';
        foreach ($path_elements as $path_element) {
            $path .= $path_element . "/";
        }
        $url = $CI->config->item('rest_api', 'aleph') . "/rest-dlf/" . $path;
        $url = $this->appendQueryString($url, $params);
        $result = $this->doHTTPRequest($url, $method, $body);
        $replyCode = (string) $result->{'reply-code'};

        if ($replyCode != "0000") {
            $replyText = (string) $result->{'reply-text'};
            $ex = new Exception($replyText . " code " . $replyCode);
            throw $ex;
        }

        return $result;
    }

    protected function doHTTPRequest($url, $method = 'GET', $body = null)
    {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        if ($body != null) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
        }
        $answer = curl_exec($ch);
        if (!$answer) {
            $error = curl_error($ch);
            $message = "HTTP request failed with message: $error, URL: '$url'.";
            dump($message);
            throw new Exception($message);
        }

        curl_close($ch);
        $answer = str_replace('xmlns=', 'ns=', $answer);
        $result = @simplexml_load_string($answer);
        if (!$result) {
            throw new Exception("XML is not valid, URL: '$url'.");
        }
        return $result;
    }

    protected function appendQueryString($url, $params)
    {
        $sep = (strpos($url, "?") === false) ? '?' : '&';
        if ($params != null) {
            foreach ($params as $key => $value) {
                $url .= $sep . $key . "=" . urlencode($value);
                $sep = "&";
            }
        }
        return $url;
    }

    protected function createLibraryXml($params, $library)
    {
        $date = date('Ymd');

        return "<?xml version='1.0'?>
                <p-file-20>
                    <patron-record>
                        <z303>
                            <match-id-type>00</match-id-type>
                            <match-id>" . $params['id'] . "</match-id>
                            <record-action>A</record-action>
                            <z303-id>" . $params['id'] . "</z303-id>
                        </z303>
                        <z305>
                            <record-action>A</record-action>
                            <z305-id>" . $params['id'] . "</z305-id>
                            <z305-sub-library>" . $library . "</z305-sub-library>
                            <z305-open-date>" . $date . "</z305-open-date>
                            <z305-update-date>" . $date . "</z305-update-date>
                            <z305-bor-status>" . $params['status'] . "</z305-bor-status>
                        </z305>
                    </patron-record>
                </p-file-20>";
    }

    protected function createPatronXml($params)
    {
        $date = date('Ymd');
        $params['birth_date'] = date('Ymd', strtotime($params['birth_date']));

        $xml = "<?xml version='1.0'?>" .
            "<p-file-20>
            <patron-record>
                <z303>
                    <match-id-type>00</match-id-type>
                    <match-id>" . $params['id'] . "</match-id>
                    <record-action>" . $params['action'] . "</record-action>
                    <z303-id>" . $params['id'] . "</z303-id>
                    <z303-proxy-for-id></z303-proxy-for-id>
                    <z303-primary-id></z303-primary-id>
                    <z303-name-key>" . $params['lastname'] . " " . $params['firstname'] . " " . $params['id'] . "</z303-name-key>
                    <z303-user-type></z303-user-type>
                    <z303-user-library>KNA50</z303-user-library>
                    <z303-home-library>" . $params['home_library'] . "</z303-home-library>
                    <z303-open-date>" . $date . "</z303-open-date>
                    <z303-update-date>" . $date . "</z303-update-date>
                    <z303-con-lng>" . $params['con_lng'] . "</z303-con-lng>
                    <z303-alpha>L</z303-alpha>
                    <z303-name>" . $params['lastname'] . " " . $params['firstname'] . "</z303-name>
                    <z303-birth-date>" . $params['birth_date'] . "</z303-birth-date>
                    <z303-export-consent>" . $params['export_consent'] . "</z303-export-consent>
                    <z303-send-all-letters>Y</z303-send-all-letters>
                    <z303-plain-html>P</z303-plain-html>
                    <z303-want-sms>N</z303-want-sms>
                    <z303-gender></z303-gender>
                    <z303-birthplace></z303-birthplace>
                    <z303-last-name>" . $params['lastname'] . "</z303-last-name>
                    <z303-first-name>" . $params['firstname'] . "</z303-first-name>
                </z303>
                <z304>
                    <record-action>" . $params['action'] . "</record-action>
                    <z304-id>" . $params['id'] . "</z304-id>
                    <z304-sequence>01</z304-sequence>
                    <z304-address-0>" . $params['lastname'] . " " . $params['firstname'] . "</z304-address-0>
                    <z304-address-1>" . $params['ident_address1'] . "</z304-address-1>
                    <z304-address-2>" . $params['ident_address2'] . "</z304-address-2>
                    <z304-zip>" . $params['ident_zip'] . "</z304-zip>
                    <z304-email-address>" . $params['email'] . "</z304-email-address>
                    <z304-date-from>" . $date . "</z304-date-from>
                    <z304-address-type>01</z304-address-type>
                    <z304-sms-number>" . $params['sms_number'] . "</z304-sms-number>
                    <z304-cat-name>VYPTJ</z304-cat-name>
                </z304>";

        if (!empty($params['contact_address1']) || !empty($params['contact_address2']) || !empty($params['contact_zip'])) {
            $xml .=
                "<z304>
                    <record-action>" . $params['action'] . "</record-action>
                    <z304-id>" . $params['id'] . "</z304-id>
                    <z304-sequence>02</z304-sequence>
                    <z304-address-0>" . $params['lastname'] . " " . $params['firstname'] . "</z304-address-0>
                    <z304-address-1>" . $params['contact_address1'] . "</z304-address-1>
                    <z304-address-2>" . $params['contact_address2'] . "</z304-address-2>
                    <z304-zip>" . $params['contact_zip'] . "</z304-zip>
                    <z304-email-address>" . $params['email'] . "</z304-email-address>
                    <z304-date-from>" . $date . "</z304-date-from>
                    <z304-address-type>02</z304-address-type>
                    <z304-sms-number>" . $params['sms_number'] . "</z304-sms-number>
                </z304>";
        }

        $xml .=
            "<z305>
                <record-action>" . $params['action'] . "</record-action>
                <z305-id>" . $params['id'] . "</z305-id>
                <z305-sub-library>" . $params['home_library'] . "</z305-sub-library>
                <z305-open-date>" . $date . "</z305-open-date>
                <z305-update-date>" . $date . "</z305-update-date>
                <z305-bor-status>" . $params['status'] . "</z305-bor-status>
            </z305>
            <z308>
                <record-action>" . $params['action'] . "</record-action>
                <z308-key-type>00</z308-key-type>
                <z308-key-data>" . $params['id'] . "</z308-key-data>
                <z308-verification>" . $params['password'] . "</z308-verification>
                <z308-verification-type>00</z308-verification-type>
                <z308-id>" . $params['id'] . "</z308-id>
                <z308-status>AC</z308-status>
                <z308-encryption>N</z308-encryption>
            </z308>
            <z308>
                <record-action>" . $params['action'] . "</record-action>
                <z308-key-type>01</z308-key-type>
                <z308-key-data>" . $params['barcode'] . "</z308-key-data>
                <z308-verification>" . $params['password'] . "</z308-verification>
                <z308-verification-type>00</z308-verification-type>
                <z308-id>" . $params['id'] . "</z308-id>
                <z308-status>AC</z308-status>
                <z308-encryption>N</z308-encryption>
            </z308>";

        if (!empty($params['rfid'])) {
            $xml .=
                "<z308>
                    <record-action>" . $params['action'] . "</record-action>
                    <z308-key-type>03</z308-key-type>
                    <z308-key-data>" . $params['rfid'] . "</z308-key-data>
                    <z308-user-library>KNA50</z308-user-library>
                    <z308-verification-type>00</z308-verification-type>
                    <z308-id>" . $params['id'] . "</z308-id>
                    <z308-status>AC</z308-status>
                    <z308-encryption>N</z308-encryption>
                </z308>";
        }

        if (!empty($params['card_number'])) {
            $xml .=
                "<z308>
                    <record-action>" . $params['action'] . "</record-action>
                    <z308-key-type>05</z308-key-type>
                    <z308-key-data>" . $params['card_number'] . "</z308-key-data>
                    <z308-user-library>KNA50</z308-user-library>
                    <z308-verification-type>00</z308-verification-type>
                    <z308-id>" . $params['id'] . "</z308-id>
                    <z308-status>AC</z308-status>
                    <z308-encryption>N</z308-encryption>
                </z308>";
        }

        $xml .= "</patron-record>
        </p-file-20>";

        return $xml;
    }

    protected function updatePatronXml($params)
    {
        $date = date('Ymd');

        $params['birth_date'] = date('Ymd', strtotime($params['birth_date']));

        $xml = '<?xml version="1.0" encoding="UTF-8"?>' .
            "<p-file-20>
            <patron-record>
                <z303>
                    <match-id-type>00</match-id-type>
                    <match-id>" . $params['id'] . "</match-id>
                    <record-action>" . $params['action'] . "</record-action>
                    <z303-id>" . $params['id'] . "</z303-id>
                    <z303-proxy-for-id></z303-proxy-for-id>
                    <z303-primary-id></z303-primary-id>
                    <z303-name-key>" . $params['lastname'] . " " . $params['firstname'] . " " . $params['id'] . "</z303-name-key>
                    <z303-user-type></z303-user-type>
                    <z303-user-library>KNA50</z303-user-library>
                    <z303-home-library>" . $params['home_library'] . "</z303-home-library>
                    <z303-update-date>" . $date . "</z303-update-date>
                    <z303-con-lng>" . $params['con_lng'] . "</z303-con-lng>
                    <z303-alpha>L</z303-alpha>
                    <z303-name>" . $params['lastname'] . " " . $params['firstname'] . "</z303-name>
                    <z303-birth-date>" . $params['birth_date'] . "</z303-birth-date>
                    <z303-export-consent>" . $params['export_consent'] . "</z303-export-consent>
                    <z303-send-all-letters>Y</z303-send-all-letters>
                    <z303-plain-html>P</z303-plain-html>
                    <z303-want-sms>N</z303-want-sms>
                    <z303-gender></z303-gender>
                    <z303-birthplace></z303-birthplace>
                    <z303-last-name>" . $params['lastname'] . "</z303-last-name>
                    <z303-first-name>" . $params['firstname'] . "</z303-first-name>
                </z303>
                <z304>
                    <record-action>" . $params['action'] . "</record-action>
                    <z304-id>" . $params['id'] . "</z304-id>
                    <z304-sequence>01</z304-sequence>
                    <z304-address-0>" . $params['lastname'] . " " . $params['firstname'] . "</z304-address-0>
                    <z304-address-1>" . $params['ident_address1'] . "</z304-address-1>
                    <z304-address-2>" . $params['ident_address2'] . "</z304-address-2>
                    <z304-zip>" . $params['ident_zip'] . "</z304-zip>
                    <z304-email-address>" . $params['email'] . "</z304-email-address>
                    <z304-date-from>" . $date . "</z304-date-from>
                    <z304-address-type>01</z304-address-type>
                    <z304-sms-number>" . $params['sms_number'] . "</z304-sms-number>
                </z304>";

        if (!empty($params['contact_address1']) || !empty($params['contact_address2']) || !empty($params['contact_zip'])) {
            $xml .=
                "<z304>
                    <record-action>" . $params['action'] . "</record-action>
                    <z304-id>" . $params['id'] . "</z304-id>
                    <z304-sequence>02</z304-sequence>
                    <z304-address-0>" . $params['lastname'] . " " . $params['firstname'] . "</z304-address-0>
                    <z304-address-1>" . $params['contact_address1'] . "</z304-address-1>
                    <z304-address-2>" . $params['contact_address2'] . "</z304-address-2>
                    <z304-zip>" . $params['contact_zip'] . "</z304-zip>
                    <z304-email-address>" . $params['email'] . "</z304-email-address>
                    <z304-date-from>" . $date . "</z304-date-from>
                    <z304-address-type>02</z304-address-type>
                    <z304-sms-number>" . $params['sms_number'] . "</z304-sms-number>
                </z304>";
        }

        if (!empty($params['rfid'])) {
            $xml .=
                "<z308>
                    <record-action>" . $params['action'] . "</record-action>
                    <z308-key-type>03</z308-key-type>
                    <z308-key-data>" . $params['rfid'] . "</z308-key-data>
                    <z308-user-library>KNA50</z308-user-library>
                    <z308-verification-type>00</z308-verification-type>
                    <z308-id>" . $params['id'] . "</z308-id>
                    <z308-status>AC</z308-status>
                    <z308-encryption>N</z308-encryption>
                </z308>";
        }

        if (!empty($params['card_number'])) {
            $xml .=
                "<z308>
                    <record-action>" . $params['action'] . "</record-action>
                    <z308-key-type>05</z308-key-type>
                    <z308-key-data>" . $params['card_number'] . "</z308-key-data>
                    <z308-user-library>KNA50</z308-user-library>
                    <z308-verification-type>00</z308-verification-type>
                    <z308-id>" . $params['id'] . "</z308-id>
                    <z308-status>AC</z308-status>
                    <z308-encryption>N</z308-encryption>
                </z308>";
        }

        $xml .= "</patron-record>
        </p-file-20>";

        return $xml;
    }

    protected function createPasswordXml($id, $barcode, $password)
    {
        return "<?xml version='1.0'?>
            <p-file-20>
                <patron-record>
                    <z303>
                        <match-id-type>00</match-id-type>
                        <match-id>" . $id . "</match-id>
                        <record-action>U</record-action>
                        <z303-id>" . $id . "</z303-id>
                    </z303>
                    <z308>
                        <record-action>U</record-action>
                        <z308-key-type>01</z308-key-type>
                        <z308-key-data>" . $barcode . "</z308-key-data>
                        <z308-user-library>KNA50</z308-user-library>
                        <z308-verification>" . $password . "</z308-verification>
                        <z308-verification-type>00</z308-verification-type>
                        <z308-id>" . $id . "</z308-id>
                        <z308-status>AC</z308-status>
                        <z308-encryption>N</z308-encryption>
                    </z308>
                </patron-record>
            </p-file-20>";
    }

    protected function createItemXml($params)
    {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" .
            "<z30><z30-doc-number>" . $params['sysno'] . "</z30-doc-number>" .
            "<z30-item-sequence>1.0</z30-item-sequence>" .
            "<z30-barcode>" . $params['barcode'] . "</z30-barcode>" .
            "<z30-sub-library>KNAV</z30-sub-library>" .
            "<z30-material>BOOK</z30-material>" .
            "<z30-item-status>" . $params['type'] . "</z30-item-status>" .
            "<z30-open-date/>" .
            "<z30-update-date/>" .
            "<z30-cataloger>OIT</z30-cataloger>" .
            "<z30-date-last-return>0</z30-date-last-return>" .
            "<z30-hour-last-return>0</z30-hour-last-return>" .
            "<z30-ip-last-return/>" .
            "<z30-no-loans>000</z30-no-loans>" .
            "<z30-alpha>L</z30-alpha>" .
            "<z30-collection/>" .
            "<z30-call-no-type/>" .
            "<z30-call-no>studovna</z30-call-no>" .
            "<z30-call-no-key>01</z30-call-no-key>" .
            "<z30-call-no-2-type />" .
            "<z30-call-no-2 />" .
            "<z30-call-no-2-key />" .
            "<z30-description>" . $params['description'] . "</z30-description>" .
            "<z30-note-opac />" .
            "<z30-note-circulation />" .
            "<z30-note-internal></z30-note-internal>" .
            "<z30-order-number />" .
            "<z30-inventory-number />" .
            "<z30-inventory-number-date />" .
            "<z30-last-shelf-report-date>00000000</z30-last-shelf-report-date>" .
            "<z30-price />" .
            "<z30-shelf-report-number />" .
            "<z30-on-shelf-date>00000000</z30-on-shelf-date>" .
            "<z30-on-shelf-seq>000000</z30-on-shelf-seq>" .
            "<z30-doc-number-2>000000000</z30-doc-number-2>" .
            "<z30-schedule-sequence-2>00000</z30-schedule-sequence-2>" .
            "<z30-copy-sequence-2>00000</z30-copy-sequence-2>" .
            "<z30-vendor-code />" .
            "<z30-invoice-number />" .
            "<z30-line-number>00000</z30-line-number>" .
            "<z30-pages />" .
            "<z30-issue-date />" .
            "<z30-expected-arrival-date />" .
            "<z30-arrival-date />" .
            "<z30-item-statistic />" .
            "<z30-item-process-status/>" .
            "<z30-copy-id/>" .
            "<z30-hol-doc-number>000000000</z30-hol-doc-number>" .
            "<z30-temp-location>No</z30-temp-location>" .
            "<z30-enumeration-a />" .
            "<z30-enumeration-b />" .
            "<z30-enumeration-c />" .
            "<z30-enumeration-d />" .
            "<z30-enumeration-e />" .
            "<z30-enumeration-f />" .
            "<z30-enumeration-g />" .
            "<z30-enumeration-h />" .
            "<z30-chronological-i />" .
            "<z30-chronological-j />" .
            "<z30-chronological-k />" .
            "<z30-chronological-l />" .
            "<z30-chronological-m />" .
            "<z30-supp-index-o />" .
            "<z30-85x-type />" .
            "<z30-depository-id />" .
            "<z30-linking-number>000000000</z30-linking-number>" .
            "<z30-gap-indicator />" .
            "<z30-maintenance-count>000</z30-maintenance-count>" .
            "<z30-process-status-date/>" .
            "</z30>";
    }

    protected function createUpdateXmlForRemoteUsers($userid, $status)
    {
        $date = date('Ymd');

        return "<?xml version='1.0'?>
        <p-file-20>
            <patron-record>
                <z303>
                    <match-id-type>00</match-id-type>
                    <match-id>" . $userid . "</match-id>
                    <record-action>A</record-action>
                    <z303-id>" . $userid . "</z303-id>
                </z303>
                <z305>
                    <record-action>A</record-action>
                    <z305-id>" . $userid . "</z305-id>
                    <z305-sub-library>KNA50</z305-sub-library>
                    <z305-bor-status>" . $status . "</z305-bor-status>
                    <z305-registration-date>" . $date . "</z305-registration-date>
                </z305>
                <z305>
                    <record-action>A</record-action>
                    <z305-id>" . $userid . "</z305-id>
                    <z305-sub-library>KNAV</z305-sub-library>
                    <z305-bor-status>" . $status . "</z305-bor-status>
                    <z305-registration-date>" . $date . "</z305-registration-date>
                </z305>
                <z305>
                    <record-action>A</record-action>
                    <z305-id>" . $userid . "</z305-id>
                    <z305-sub-library>KNAVD</z305-sub-library>
                    <z305-bor-status>" . $status . "</z305-bor-status>
                    <z305-registration-date>" . $date . "</z305-registration-date>
                </z305>
                <z305>
                    <record-action>A</record-action>
                    <z305-id>" . $userid . "</z305-id>
                    <z305-sub-library>KNAVP</z305-sub-library>
                    <z305-bor-status>" . $status . "</z305-bor-status>
                    <z305-registration-date>" . $date . "</z305-registration-date>
                </z305>
            </patron-record>
        </p-file-20>";
    }

    protected function parseId($id)
    {
        if (count($this->bib) == 1) {
            return [$this->bib[0], $id];
        }

        return explode('-', $id);
    }
}
