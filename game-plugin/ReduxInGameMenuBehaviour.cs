using System;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.XR;

namespace Redux.GamePlugin
{
    /// <summary>
    /// Unity-side REDUX menu template. It must be loaded by a compatible mod loader
    /// inside the target game; the Android companion APK cannot inject this component.
    /// </summary>
    public sealed class ReduxInGameMenuBehaviour : MonoBehaviour
    {
        private Canvas menuCanvas;
        private RectTransform panel;
        private Text statusText;
        private InputDevice leftDevice;
        private InputDevice rightDevice;
        private bool menuOpen;
        private bool handMode = true;
        private bool previousOpenChord;
        private bool previousRightTrigger;
        private string selectedProfile = "REDUX";
        private string selectedCategory = "MOVEMENT";

        public static void Install()
        {
            if (FindObjectOfType<ReduxInGameMenuBehaviour>() != null) return;
            var host = new GameObject("REDUX.InGameMenu");
            DontDestroyOnLoad(host);
            host.AddComponent<ReduxInGameMenuBehaviour>();
        }

        private void Start()
        {
            RefreshDevices();
            BuildMenu();
        }

        private void Update()
        {
            if (!leftDevice.isValid || !rightDevice.isValid) RefreshDevices();

            bool trigger = IsTriggerPressed(leftDevice);
            bool grip = IsGripPressed(leftDevice);
            bool chord = trigger && grip;

            // One toggle per press; continuing to hold the chord does not flicker the menu.
            if (chord && !previousOpenChord)
            {
                menuOpen = !menuOpen;
                if (menuCanvas != null) menuCanvas.enabled = menuOpen;
            }
            previousOpenChord = chord;

            if (!menuOpen || menuCanvas == null) return;
            PositionMenu();

            bool rightTrigger = IsTriggerPressed(rightDevice);
            if (rightTrigger && !previousRightTrigger) ClickMenuWithRightController();
            previousRightTrigger = rightTrigger;
        }

        private void RefreshDevices()
        {
            leftDevice = InputDevices.GetDeviceAtXRNode(XRNode.LeftHand);
            rightDevice = InputDevices.GetDeviceAtXRNode(XRNode.RightHand);
        }

        private static bool IsTriggerPressed(InputDevice device)
        {
            if (!device.isValid) return false;
            bool pressed;
            if (device.TryGetFeatureValue(CommonUsages.triggerButton, out pressed) && pressed) return true;
            float value;
            return device.TryGetFeatureValue(CommonUsages.trigger, out value) && value >= 0.72f;
        }

        private static bool IsGripPressed(InputDevice device)
        {
            if (!device.isValid) return false;
            bool pressed;
            if (device.TryGetFeatureValue(CommonUsages.gripButton, out pressed) && pressed) return true;
            float value;
            return device.TryGetFeatureValue(CommonUsages.grip, out value) && value >= 0.72f;
        }

        private Transform TrackingOrigin()
        {
            Camera cam = Camera.main;
            if (cam == null) return transform;
            Transform root = cam.transform;
            while (root.parent != null) root = root.parent;
            return root;
        }

        private void PositionMenu()
        {
            Camera cam = Camera.main;
            if (cam == null || menuCanvas == null) return;

            Vector3 position;
            if (handMode && leftDevice.isValid)
            {
                Vector3 localPosition;
                Quaternion localRotation;
                Transform origin = TrackingOrigin();
                if (leftDevice.TryGetFeatureValue(CommonUsages.devicePosition, out localPosition) &&
                    leftDevice.TryGetFeatureValue(CommonUsages.deviceRotation, out localRotation))
                {
                    Vector3 handWorld = origin.TransformPoint(localPosition);
                    Quaternion handWorldRotation = origin.rotation * localRotation;
                    position = handWorld + handWorldRotation * new Vector3(0.22f, 0.08f, 0.12f);
                }
                else
                {
                    position = cam.transform.position + cam.transform.right * -0.35f
                        + cam.transform.forward * 0.75f - cam.transform.up * 0.12f;
                }
            }
            else
            {
                position = cam.transform.position + cam.transform.forward * 1.05f
                    + cam.transform.right * 0.12f - cam.transform.up * 0.08f;
            }

            menuCanvas.transform.position = position;
            menuCanvas.transform.rotation = Quaternion.LookRotation(cam.transform.position - position, cam.transform.up);
        }

        private void ClickMenuWithRightController()
        {
            Vector3 localPosition;
            Quaternion localRotation;
            if (!rightDevice.TryGetFeatureValue(CommonUsages.devicePosition, out localPosition) ||
                !rightDevice.TryGetFeatureValue(CommonUsages.deviceRotation, out localRotation)) return;

            Transform origin = TrackingOrigin();
            Vector3 rayOrigin = origin.TransformPoint(localPosition);
            Vector3 rayDirection = origin.rotation * localRotation * Vector3.forward;
            RaycastHit hit;
            if (!Physics.Raycast(rayOrigin, rayDirection, out hit, 4.0f)) return;

            Button button = hit.collider.GetComponent<Button>();
            if (button != null && button.interactable) button.onClick.Invoke();
        }

        private void BuildMenu()
        {
            if (menuCanvas != null) Destroy(menuCanvas.gameObject);

            GameObject canvasObject = new GameObject("REDUX World Menu", typeof(RectTransform), typeof(Canvas), typeof(CanvasScaler));
            canvasObject.transform.SetParent(transform, false);
            menuCanvas = canvasObject.GetComponent<Canvas>();
            menuCanvas.renderMode = RenderMode.WorldSpace;
            menuCanvas.worldCamera = Camera.main;
            menuCanvas.enabled = menuOpen;

            RectTransform canvasRect = canvasObject.GetComponent<RectTransform>();
            canvasRect.sizeDelta = new Vector2(680f, 1120f);
            canvasRect.localScale = Vector3.one * 0.0009f;

            CanvasScaler scaler = canvasObject.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ConstantPixelSize;
            scaler.scaleFactor = 1f;

            GameObject panelObject = new GameObject("Panel", typeof(RectTransform), typeof(Image), typeof(VerticalLayoutGroup));
            panelObject.transform.SetParent(canvasObject.transform, false);
            panel = panelObject.GetComponent<RectTransform>();
            panel.anchorMin = Vector2.zero;
            panel.anchorMax = Vector2.one;
            panel.offsetMin = Vector2.zero;
            panel.offsetMax = Vector2.zero;

            Image background = panelObject.GetComponent<Image>();
            background.color = new Color(0.035f, 0.035f, 0.04f, 0.97f);
            Outline outline = panelObject.AddComponent<Outline>();
            outline.effectColor = new Color(0.95f, 0.04f, 0.12f, 1f);
            outline.effectDistance = new Vector2(4f, -4f);

            VerticalLayoutGroup layout = panelObject.GetComponent<VerticalLayoutGroup>();
            layout.padding = new RectOffset(22, 22, 18, 18);
            layout.spacing = 7f;
            layout.childAlignment = TextAnchor.UpperCenter;
            layout.childControlWidth = true;
            layout.childControlHeight = true;
            layout.childForceExpandWidth = true;
            layout.childForceExpandHeight = false;

            AddLabel("REDUX", 34, new Color(1f, 0.06f, 0.15f), 54);
            AddLabel("HOLD LEFT TRIGGER + GRIP TO OPEN MENU", 20, Color.white, 58);
            AddLabel("AIM WITH RIGHT CONTROLLER • PRESS RIGHT TRIGGER TO SELECT", 16, Color.gray, 40);
            AddLabel(handMode ? "MODE: HAND MENU" : "MODE: FLOATING COMPUTER", 18, Color.gray, 36);

            AddButton("SWITCH TO " + (handMode ? "FLOATING COMPUTER" : "HAND MENU"), () =>
            {
                handMode = !handMode;
                BuildMenu();
            }, 54);

            AddLabel("SELECT PROFILE", 19, new Color(1f, 0.12f, 0.2f), 34);
            AddButton("REDUX", () => SelectProfile("REDUX"), 44);
            AddButton("LUDEX / LUNEX", () => SelectProfile("LUNEX"), 44);
            AddButton("STRIPES ADB", () => SelectProfile("STRIPES"), 44);
            AddButton("DELTA", () => SelectProfile("DELTA"), 44);
            AddButton("MOONLIT V2", () => SelectProfile("MOONLIT"), 44);

            AddLabel("CATEGORY: " + selectedCategory, 18, new Color(1f, 0.12f, 0.2f), 34);
            AddButton("MOVEMENT", () => SelectCategory("MOVEMENT"), 42);
            AddButton("VISUALS", () => SelectCategory("VISUALS"), 42);
            AddButton("PLAYER", () => SelectCategory("PLAYER"), 42);
            AddButton("SETTINGS", () => SelectCategory("SETTINGS"), 42);

            AddLabel("OPTIONS • " + selectedProfile, 18, Color.white, 34);
            if (selectedCategory == "VISUALS")
            {
                AddOption("GHOST MONKE");
                AddOption("TRACERS");
                AddOption("PLAYER HIGHLIGHTS");
            }
            else if (selectedCategory == "PLAYER")
            {
                AddOption("LONG ARMS");
                AddOption("WALL WALK");
                AddOption("SPIDER-MAN");
            }
            else if (selectedCategory == "SETTINGS")
            {
                AddOption("MENU POSITION");
                AddOption("CONTROLLER SHORTCUT");
                AddOption("PROFILE SETTINGS");
            }
            else
            {
                AddOption("FLY");
                AddOption("JOYSTICK FLY");
                AddOption("VELOCITY FLY");
            }

            statusText = AddLabel("MENU READY • OPTIONS REQUIRE A GAME-SPECIFIC IMPLEMENTATION", 15,
                new Color(0.72f, 0.72f, 0.72f), 64);
            PositionMenu();
        }

        private Text AddLabel(string value, int fontSize, Color color, float height)
        {
            GameObject item = new GameObject("Label", typeof(RectTransform), typeof(Text), typeof(LayoutElement));
            item.transform.SetParent(panel, false);
            Text text = item.GetComponent<Text>();
            text.text = value;
            text.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            text.fontSize = fontSize;
            text.color = color;
            text.alignment = TextAnchor.MiddleCenter;
            text.horizontalOverflow = HorizontalWrapMode.Wrap;
            text.verticalOverflow = VerticalWrapMode.Overflow;
            text.fontStyle = FontStyle.Bold;
            text.raycastTarget = false;
            item.GetComponent<LayoutElement>().preferredHeight = height;
            return text;
        }

        private void AddButton(string label, Action action, float height)
        {
            GameObject item = new GameObject("Button - " + label, typeof(RectTransform), typeof(Image), typeof(Button), typeof(LayoutElement), typeof(BoxCollider));
            item.transform.SetParent(panel, false);
            Image image = item.GetComponent<Image>();
            image.color = new Color(0.10f, 0.10f, 0.12f, 1f);

            Button button = item.GetComponent<Button>();
            button.targetGraphic = image;
            button.onClick.AddListener(() => action());

            LayoutElement element = item.GetComponent<LayoutElement>();
            element.preferredHeight = height;

            BoxCollider collider = item.GetComponent<BoxCollider>();
            collider.size = new Vector3(630f, height, 18f);

            GameObject labelObject = new GameObject("Text", typeof(RectTransform), typeof(Text));
            labelObject.transform.SetParent(item.transform, false);
            RectTransform labelRect = labelObject.GetComponent<RectTransform>();
            labelRect.anchorMin = Vector2.zero;
            labelRect.anchorMax = Vector2.one;
            labelRect.offsetMin = new Vector2(8f, 0f);
            labelRect.offsetMax = new Vector2(-8f, 0f);

            Text text = labelObject.GetComponent<Text>();
            text.text = label;
            text.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            text.fontSize = 20;
            text.fontStyle = FontStyle.Bold;
            text.alignment = TextAnchor.MiddleCenter;
            text.color = Color.white;
            text.raycastTarget = false;
        }

        private void AddOption(string name)
        {
            AddButton(name + "  •  PREVIEW ONLY", () =>
            {
                if (statusText != null)
                    statusText.text = name + " is selected in the menu UI, but its game behavior is not implemented.";
            }, 44);
        }

        private void SelectProfile(string profile)
        {
            selectedProfile = profile;
            BuildMenu();
        }

        private void SelectCategory(string category)
        {
            selectedCategory = category;
            BuildMenu();
        }
    }
}
